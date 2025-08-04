#include <android/log.h>
#include <jni.h>
#include <iomanip>
#include <math.h>
#include <string>
#include <vector>
#include <unistd.h>
#include "llama.h"
#include "common.h"
#include "chat.h"
#define JSON_ASSERT GGML_ASSERT
#include "nlohmann/json.hpp"

using json = nlohmann::ordered_json;

template <typename T>
static T json_value(const json & body, const std::string & key, const T & default_value) {
    // Fallback null to default value
    if (body.contains(key) && !body.at(key).is_null()) {
        try {
            return body.at(key);
        } catch (NLOHMANN_JSON_NAMESPACE::detail::type_error const &) {

            return default_value;
        }
    } else {
        return default_value;
    }
}

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("llama-android");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("llama-android")
//      }
//    }

#define TAG "llama-android.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

jclass la_int_var;
jmethodID la_int_var_value;
jmethodID la_int_var_inc;

std::string cached_token_chars;

bool is_valid_utf8(const char * string) {
    if (!string) {
        return true;
    }

    const unsigned char * bytes = (const unsigned char *)string;
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            // U+0000 to U+007F
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // U+0080 to U+07FF
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            // U+0800 to U+FFFF
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            // U+10000 to U+10FFFF
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }

    return true;
}

std::string mapListToJSONString(JNIEnv *env, jobjectArray allMessages) {
    json jsonArray = json::array();

    jsize arrayLength = env->GetArrayLength(allMessages);
    for (jsize i = 0; i < arrayLength; ++i) {
        // Get the individual message from the array
        jobject messageObj = env->GetObjectArrayElement(allMessages, i);
        if (!messageObj) {
            LOGe("Error: Received null jobject at index %d", i);
            continue;
        }

        // Check if the object is a Map
        jclass mapClass = env->FindClass("java/util/Map");
        if (!env->IsInstanceOf(messageObj, mapClass)) {
            LOGe("Error: Object is not a Map at index %d", i);
            env->DeleteLocalRef(messageObj);
            continue;
        }

        // Get Map methods
        jmethodID getMethod = env->GetMethodID(mapClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        jmethodID keySetMethod = env->GetMethodID(mapClass, "keySet", "()Ljava/util/Set;");
        if (!getMethod || !keySetMethod) {
            LOGe("Error: Could not find Map methods");
            env->DeleteLocalRef(messageObj);
            continue;
        }

        // Create a JSON object for this map
        json jsonMsg;

        // Get role
        jstring roleKey = env->NewStringUTF("role");
        jobject roleObj = env->CallObjectMethod(messageObj, getMethod, roleKey);
        if (roleObj) {
            const char* roleStr = env->GetStringUTFChars((jstring)roleObj, nullptr);
            jsonMsg["role"] = roleStr;
            env->ReleaseStringUTFChars((jstring)roleObj, roleStr);
        }

        // Get content
        jstring contentKey = env->NewStringUTF("content");
        jobject contentObj = env->CallObjectMethod(messageObj, getMethod, contentKey);
        if (contentObj) {
            const char* contentStr = env->GetStringUTFChars((jstring)contentObj, nullptr);
            jsonMsg["content"] = contentStr;
            env->ReleaseStringUTFChars((jstring)contentObj, contentStr);
        }

        // Add to array if both role and content were successfully extracted
        if (!jsonMsg.empty()) {
            jsonArray.push_back(jsonMsg);
        }

        // Clean up local references
        env->DeleteLocalRef(messageObj);
    }

    return jsonArray.dump();
}

static void log_callback(ggml_log_level level, const char * fmt, void * data) {
    if (level == GGML_LOG_LEVEL_ERROR)     __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_INFO) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_WARN) __android_log_print(ANDROID_LOG_WARN, TAG, fmt, data);
    else __android_log_print(ANDROID_LOG_DEFAULT, TAG, fmt, data);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_load_1model(JNIEnv *env, jobject, jstring filename) {
    llama_model_params model_params = llama_model_default_params();

    auto path_to_model = env->GetStringUTFChars(filename, 0);
    LOGi("Loading model from %s", path_to_model);

    auto model = llama_model_load_from_file(path_to_model, model_params);
    env->ReleaseStringUTFChars(filename, path_to_model);

    if (!model) {
        LOGe("load_model() failed");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "load_model() failed");
        return 0;
    }

    return reinterpret_cast<jlong>(model);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1model(JNIEnv *, jobject, jlong model) {
    llama_model_free(reinterpret_cast<llama_model *>(model));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1context(JNIEnv *env, jobject, jlong jmodel, jint userThreads) {
    auto model = reinterpret_cast<llama_model *>(jmodel);

    if (!model) {
        LOGe("new_context(): model cannot be null");
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Model cannot be null");
        return 0;
    }

    int n_threads = std::max(1, std::min(8, (int) sysconf(_SC_NPROCESSORS_ONLN) - 2));
    LOGi("Using %d threads", n_threads);
    int userSpecifiedThreads = (userThreads > 0) ? std::min(9, std::max(1, userThreads))
                                                 : std::max(1, std::min(8, (int) sysconf(_SC_NPROCESSORS_ONLN) - 2));
    LOGi("Using %d threads for computation", userSpecifiedThreads);
    llama_context_params ctx_params = llama_context_default_params();

    ctx_params.n_ctx           = 32768;  // Increased to match Qwen3 context length
    ctx_params.n_threads       = userSpecifiedThreads;
    ctx_params.n_threads_batch = n_threads;
    LOGi("Checking my threads %d", ctx_params.n_threads);

    llama_context * context = llama_init_from_model(model, ctx_params);

    if (!context) {
        LOGe("llama_new_context_with_model() returned null)");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "llama_new_context_with_model() returned null)");
        return 0;
    }

    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1context(JNIEnv *, jobject, jlong context) {
    llama_free(reinterpret_cast<llama_context *>(context));
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_backend_1free(JNIEnv *, jobject) {
    llama_backend_free();
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_log_1to_1android(JNIEnv *, jobject) {
    llama_log_set(log_callback, NULL);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_bench_1model(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong model_pointer,
        jlong batch_pointer,
        jint pp,
        jint tg,
        jint pl,
        jint nr
        ) {
    auto pp_avg = 0.0;
    auto tg_avg = 0.0;
    auto pp_std = 0.0;
    auto tg_std = 0.0;

    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto model = reinterpret_cast<llama_model *>(model_pointer);
    const auto batch = reinterpret_cast<llama_batch *>(batch_pointer);

    const int n_ctx = llama_n_ctx(context);

    LOGi("n_ctx = %d", n_ctx);

    int i, j;
    int nri;
    for (nri = 0; nri < nr; nri++) {
        LOGi("Benchmark prompt processing (pp)");

        common_batch_clear(*batch);

        const int n_tokens = pp;
        for (i = 0; i < n_tokens; i++) {
            common_batch_add(*batch, 0, i, { 0 }, false);
        }

        batch->logits[batch->n_tokens - 1] = true;
        llama_memory_clear(llama_get_memory(context), true);

        const auto t_pp_start = ggml_time_us();
        if (llama_decode(context, *batch) != 0) {
            LOGi("llama_decode() failed during prompt processing");
        }
        const auto t_pp_end = ggml_time_us();

        // bench text generation

        LOGi("Benchmark text generation (tg)");

        llama_memory_clear(llama_get_memory(context), true);
        const auto t_tg_start = ggml_time_us();
        for (i = 0; i < tg; i++) {

            common_batch_clear(*batch);
            for (j = 0; j < pl; j++) {
                common_batch_add(*batch, 0, i, { j }, true);
            }

            LOGi("llama_decode() text generation: %d", i);
            if (llama_decode(context, *batch) != 0) {
                LOGi("llama_decode() failed during text generation");
            }
        }

        const auto t_tg_end = ggml_time_us();

        llama_memory_clear(llama_get_memory(context), true);

        const auto t_pp = double(t_pp_end - t_pp_start) / 1000000.0;
        const auto t_tg = double(t_tg_end - t_tg_start) / 1000000.0;

        const auto speed_pp = double(pp) / t_pp;
        const auto speed_tg = double(pl * tg) / t_tg;

        pp_avg += speed_pp;
        tg_avg += speed_tg;

        pp_std += speed_pp * speed_pp;
        tg_std += speed_tg * speed_tg;

        LOGi("pp %f t/s, tg %f t/s", speed_pp, speed_tg);
    }

    pp_avg /= double(nr);
    tg_avg /= double(nr);

    if (nr > 1) {
        pp_std = sqrt(pp_std / double(nr - 1) - pp_avg * pp_avg * double(nr) / double(nr - 1));
        tg_std = sqrt(tg_std / double(nr - 1) - tg_avg * tg_avg * double(nr) / double(nr - 1));
    } else {
        pp_std = 0;
        tg_std = 0;
    }

    char model_desc[128];
    llama_model_desc(model, model_desc, sizeof(model_desc));

    const auto model_size     = double(llama_model_size(model)) / 1024.0 / 1024.0 / 1024.0;
    const auto model_n_params = double(llama_model_n_params(model)) / 1e9;

    const auto backend    = "(Android)"; // TODO: What should this be?

    std::stringstream result;
    result << std::setprecision(2);
    result << "| model | size | params | backend | test | t/s |\n";
    result << "| --- | --- | --- | --- | --- | --- |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << backend << " | pp " << pp << " | " << pp_avg << " ± " << pp_std << " |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << backend << " | tg " << tg << " | " << tg_avg << " ± " << tg_std << " |\n";

    return env->NewStringUTF(result.str().c_str());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1batch(JNIEnv *, jobject, jint n_tokens, jint embd, jint n_seq_max) {

    // Source: Copy of llama.cpp:llama_batch_init but heap-allocated.

    llama_batch *batch = new llama_batch {
        0,
        nullptr,
        nullptr,
        nullptr,
        nullptr,
        nullptr,
        nullptr,
    };

    if (embd) {
        batch->embd = (float *) malloc(sizeof(float) * n_tokens * embd);
    } else {
        batch->token = (llama_token *) malloc(sizeof(llama_token) * n_tokens);
    }

    batch->pos      = (llama_pos *)     malloc(sizeof(llama_pos)      * n_tokens);
    batch->n_seq_id = (int32_t *)       malloc(sizeof(int32_t)        * n_tokens);
    batch->seq_id   = (llama_seq_id **) malloc(sizeof(llama_seq_id *) * n_tokens);
    for (int i = 0; i < n_tokens; ++i) {
        batch->seq_id[i] = (llama_seq_id *) malloc(sizeof(llama_seq_id) * n_seq_max);
    }
    batch->logits   = (int8_t *)        malloc(sizeof(int8_t)         * n_tokens);

    return reinterpret_cast<jlong>(batch);
}

void fixed_llama_batch_free(struct llama_batch batch) {
    if (batch.token)    free(batch.token);
    if (batch.embd)     free(batch.embd);
    if (batch.pos)      free(batch.pos);

    if (batch.seq_id) {
        for (int i = 0; i < *batch.n_seq_id; ++i) {
            free(batch.seq_id[i]);
        }
        if (batch.n_seq_id) free(batch.n_seq_id);
        free(batch.seq_id);
    }
    if (batch.logits)   free(batch.logits);
}


extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1batch(JNIEnv *, jobject, jlong batch_pointer) {


    common_batch_clear(*reinterpret_cast<llama_batch *>(batch_pointer));

//    fixed_llama_batch_free(*reinterpret_cast<llama_batch *>(batch_pointer));

}

extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_new_1sampler(JNIEnv *, jobject, jfloat top_p, jint top_k, jfloat temp) {
    LOGi("my params temp=%.1f, top_p=%.1f, top_k=%d", temp, top_p, top_k);
    auto sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    // Top K handling
    if (top_k == 0) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40)); // Default value
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(top_k));
    }

    // Top P handling
    if (top_p == 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1)); // Default value
    } else {
        float adjusted_top_p = roundf(top_p * 10) / 10;
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(adjusted_top_p, 1));
    }

    // Temperature handling
    if (temp == 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.4f)); // Default value
    } else {
        float adjusted_temp = roundf(temp * 10) / 10;
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(adjusted_temp));
    }

    // Always add dist sampler
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(1234));

    return reinterpret_cast<jlong>(smpl);
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1sampler(JNIEnv *, jobject, jlong sampler_pointer) {
    llama_sampler_free(reinterpret_cast<llama_sampler *>(sampler_pointer));
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_backend_1init(JNIEnv *, jobject) {
    llama_backend_init();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_system_1info(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C"
JNIEXPORT jint JNICALL
Java_android_llama_cpp_LLamaAndroid_completion_1init(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jstring jtext,
        jint n_len
    ) {

    cached_token_chars.clear();

    const auto text = env->GetStringUTFChars(jtext, 0);
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto batch = reinterpret_cast<llama_batch *>(batch_pointer);

    const auto tokens_list = common_tokenize(context, text, 1);

    auto n_ctx = llama_n_ctx(context);
    auto n_kv_req = tokens_list.size() + (n_len - tokens_list.size());

    LOGi("n_len = %d, n_ctx = %d, n_kv_req = %d", n_len, n_ctx, n_kv_req);

    if (n_kv_req > n_ctx) {
        LOGe("error: n_kv_req > n_ctx, the required KV cache size is not big enough");
    }

    for (auto id : tokens_list) {
        LOGi("%s", common_token_to_piece(context, id).c_str());
    }

    common_batch_clear(*batch);

    // evaluate the initial prompt
    for (auto i = 0; i < tokens_list.size(); i++) {
        common_batch_add(*batch, tokens_list[i], i, { 0 }, false);
    }

    // llama_decode will output logits only for the last token of the prompt
    batch->logits[batch->n_tokens - 1] = true;

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() failed");
    }

    env->ReleaseStringUTFChars(jtext, text);

    return batch->n_tokens;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_completion_1loop(
        JNIEnv * env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jlong sampler_pointer,
        jint n_len,
        jobject intvar_ncur
) {
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto batch   = reinterpret_cast<llama_batch   *>(batch_pointer);
    const auto sampler = reinterpret_cast<llama_sampler *>(sampler_pointer);
    const auto model = llama_get_model(context);

    if (!la_int_var) la_int_var = env->GetObjectClass(intvar_ncur);
    if (!la_int_var_value) la_int_var_value = env->GetMethodID(la_int_var, "getValue", "()I");
    if (!la_int_var_inc) la_int_var_inc = env->GetMethodID(la_int_var, "inc", "()V");

    // sample the most likely token
    const auto new_token_id = llama_sampler_sample(sampler, context, -1);

    const auto eot = llama_vocab_eot(llama_model_get_vocab(model));
    LOGi("eot is: %d", eot);
    LOGi("new_token_id is: %d", new_token_id);

    const auto n_cur = env->CallIntMethod(intvar_ncur, la_int_var_value);
    if (llama_vocab_is_eog(llama_model_get_vocab(model), new_token_id) || n_cur == n_len || new_token_id == eot) {
        return nullptr;
    }

    auto new_token_chars = common_token_to_piece(context, new_token_id);
    cached_token_chars += new_token_chars;

    // Enhanced thinking token processing
    std::string filtered_chars = cached_token_chars;
    jstring new_token = nullptr;
    
    // Check for repetitive patterns that indicate stuck generation
    if (filtered_chars.length() > 100) {
        std::string last_100 = filtered_chars.substr(filtered_chars.length() - 100);
        if (last_100.find("Wait, can I help you out? No, that's the opposite") != std::string::npos ||
            last_100.find("I apologize, but I cannot") != std::string::npos ||
            last_100.find("I'm sorry, but I") != std::string::npos) {
            // Model is stuck in a loop, stop generation
            return nullptr;
        }
    }
    
    // Enhanced thinking token detection and preservation
    bool containsThinkingTokens = false;
    if (filtered_chars.find("<|im_start|>") != std::string::npos ||
        filtered_chars.find("<|user|>") != std::string::npos ||
        filtered_chars.find("<|assistant|>") != std::string::npos ||
        filtered_chars.find("<think>") != std::string::npos ||
        filtered_chars.find("</think>") != std::string::npos ||
        filtered_chars.find("Let me think") != std::string::npos ||
        filtered_chars.find("Let me analyze") != std::string::npos ||
        filtered_chars.find("I need to") != std::string::npos ||
        filtered_chars.find("First,") != std::string::npos ||
        filtered_chars.find("Step") != std::string::npos ||
        filtered_chars.find("thinking") != std::string::npos ||
        filtered_chars.find("reasoning") != std::string::npos) {
        containsThinkingTokens = true;
        LOGi("Thinking tokens detected: %s", filtered_chars.c_str());
    }
    
    // Always preserve thinking tokens and let UI handle display
    if (is_valid_utf8(filtered_chars.c_str())) {
        new_token = env->NewStringUTF(filtered_chars.c_str());
        LOGi("cached: %s, new_token_chars: `%s`, id: %d, thinking: %s", 
             filtered_chars.c_str(), new_token_chars.c_str(), new_token_id, 
             containsThinkingTokens ? "true" : "false");
        cached_token_chars.clear();
    } else {
        new_token = env->NewStringUTF("");
    }

    common_batch_clear(*batch);
    common_batch_add(*batch, new_token_id, n_cur, { 0 }, true);

    env->CallVoidMethod(intvar_ncur, la_int_var_inc);

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() returned null");
    }

    return new_token;
}

extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_kv_1cache_1clear(JNIEnv *, jobject, jlong context) {
    llama_memory_clear(llama_get_memory(reinterpret_cast<llama_context *>(context)), true);
}

// Format given chat. If tmpl is empty, we take the template from model metadata
inline std::string format_chat(const llama_model *model, const std::string &tmpl, const std::vector<json> &messages) {
    std::vector<common_chat_msg> chat;

    for (size_t i = 0; i < messages.size(); ++i) {
        const auto &curr_msg = messages[i];

        std::string role = json_value(curr_msg, "role", std::string(""));
        std::string content;

        if (curr_msg.contains("content")) {
            if (curr_msg["content"].is_string()) {
                content = curr_msg["content"].get<std::string>();
            } else if (curr_msg["content"].is_array()) {
                for (const auto &part : curr_msg["content"]) {
                    if (part.contains("text")) {
                        content += "\n" + part["text"].get<std::string>();
                    }
                }
            } else {
                throw std::runtime_error("Invalid 'content' type.");
            }
        } else {
            throw std::runtime_error("Missing 'content'.");
        }

        common_chat_msg msg;
        msg.role = role;
        msg.content = content;
        chat.push_back(msg);
    }

    // Create chat templates inputs
    common_chat_templates_inputs inputs;
    inputs.messages = chat;
    inputs.add_generation_prompt = true;
    inputs.use_jinja = true;

    // Get chat templates from model
    auto tmpls = common_chat_templates_init(model, tmpl);
    if (!tmpls) {
        throw std::runtime_error("Failed to initialize chat templates");
    }

    // Apply templates
    auto params = common_chat_templates_apply(tmpls.get(), inputs);
    LOGi("formatted_chat: '%s'\n", params.prompt.c_str());

    return params.prompt;
}


extern "C" JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_oaicompat_1completion_1param_1parse(
        JNIEnv *env, jobject, jobjectArray allMessages, jlong model, jstring chatFormat) {
    try {
        // Convert the messages to JSON
        std::string parsedData = mapListToJSONString(env, allMessages);
        // Parse and format
        std::vector<json> jsonMessages = json::parse(parsedData);
        
        LOGi("Processing %zu messages", jsonMessages.size());
        for (size_t i = 0; i < jsonMessages.size(); ++i) {
            const auto& msg = jsonMessages[i];
            std::string role = json_value(msg, "role", std::string(""));
            std::string content = json_value(msg, "content", std::string(""));
            LOGi("Message %zu: role='%s', content='%s'", i, role.c_str(), content.substr(0, 100).c_str());
        }
        
        // Extract the chat format string
        const char* chatFormatStr = env->GetStringUTFChars(chatFormat, nullptr);
        std::string chatFormatStr_cpp = std::string(chatFormatStr);
        env->ReleaseStringUTFChars(chatFormat, chatFormatStr);
        
        LOGi("Received chat format: '%s'", chatFormatStr_cpp.c_str());
        
        // Try to detect Qwen3 model and use appropriate template
        auto model_ptr = reinterpret_cast<const llama_model *>(model);
        std::string template_content = "";
        
        // Use the chat format from the UI to select the appropriate template
        if (chatFormatStr_cpp == "QWEN3") {
            // Use Qwen3 template with thinking support
            template_content = R"(
{%- if tools %}
 {{- '<|im_start|>system\n' }}
 {%- if messages[0].role == 'system' %}
 {{- messages[0].content + '\n\n' }}
 {%- endif %}
 {{- "# Tools\n\nYou may call one or more functions to assist with the user query.\n\nYou are provided with function signatures within <tools></tools> XML tags:\n<tools>" }}
 {%- for tool in tools %}
 {{- "\n" }}
 {{- tool | tojson }}
 {%- endfor %}
 {{- "\n</tools>\n\nFor each function call, return a json object with function name and arguments within <tool_call></tool_call> XML tags:\n<tool_call>\n{\"name\": <function-name>, \"arguments\": <args-json-object>}\n</tool_call><|im_end|>\n" }}
{%- else %}
{%- if messages[0].role == 'system' %}
 {{- '<|im_start|>system\n' + messages[0].content + '<|im_end|>\n' }}
 {%- endif %}
{%- endif %}
{%- set ns = namespace(multi_step_tool=true, last_query_index=messages|length - 1) %}
{%- for message in messages[::-1] %}
 {%- set index = (messages|length - 1) - loop.index0 %}
 {%- set tool_start = "<tool_response>" %}
 {%- set tool_start_length = tool_start|length %}
 {%- set start_of_message = message.content[:tool_start_length] %}
 {%- set tool_end = "</tool_response>" %}
 {%- set tool_end_length = tool_end|length %}
 {%- set start_pos = (message.content|length) - tool_end_length %}
 {%- if start_pos < 0 %}
 {%- set start_pos = 0 %}
 {%- endif %}
 {%- set end_of_message = message.content[start_pos:] %}
 {%- if ns.multi_step_tool and message.role == "user" and not(start_of_message == tool_start and end_of_message == tool_end) %}
 {%- set ns.multi_step_tool = false %}
 {%- set ns.last_query_index = index %}
 {%- endif %}
{%- endfor %}
{%- for message in messages %}
 {%- if (message.role == "user") or (message.role == "system" and not loop.first) %}
 {{- '<|im_start|>' + message.role + '\n' + message.content + '<|im_end|>' + '\n' }}
 {%- elif message.role == "assistant" %}
 {%- set content = message.content %}
 {%- set reasoning_content = '' %}
 {%- if message.reasoning_content is defined and message.reasoning_content is not none %}
 {%- set reasoning_content = message.reasoning_content %}
 {%- else %}
 {%- if '</think>' in message.content %}
 {%- set content = (message.content.split('</think>')|last).lstrip('\n') %}
{%- set reasoning_content = (message.content.split('</think>')|first).rstrip('\n') %}
{%- set reasoning_content = (reasoning_content.split('<think>')|last).lstrip('\n') %}
 {%- endif %}
 {%- endif %}
 {%- if loop.index0 > ns.last_query_index %}
 {%- if loop.last or (not loop.last and reasoning_content) %}
 {{- '<|im_start|>' + message.role + '\n<think>\n' + reasoning_content.strip('\n') + '\n</think>\n\n' + content.lstrip('\n') }}
 {%- else %}
 {{- '<|im_start|>' + message.role + '\n' + content }}
 {%- endif %}
 {%- else %}
 {{- '<|im_start|>' + message.role + '\n' + content }}
 {%- endif %}
 {%- if message.tool_calls %}
 {%- for tool_call in message.tool_calls %}
 {%- if (loop.first and content) or (not loop.first) %}
 {{- '\n' }}
 {%- endif %}
 {%- if tool_call.function %}
 {%- set tool_call = tool_call.function %}
 {%- endif %}
 {{- '<tool_call>\n{"name": "' }}
 {{- tool_call.name }}
 {{- '", "arguments": ' }}
 {%- if tool_call.arguments is string %}
 {{- tool_call.arguments }}
 {%- else %}
 {{- tool_call.arguments | tojson }}
 {%- endif %}
 {{- '}\n</tool_call>' }}
 {%- endfor %}
 {%- endif %}
 {{- '<|im_end|>\n' }}
 {%- elif message.role == "tool" %}
 {%- if loop.first or (messages[loop.index0 - 1].role != "tool") %}
 {{- '<|im_start|>user' }}
 {%- endif %}
 {{- '\n<tool_response>\n' }}
 {{- message.content }}
 {{- '\n</tool_response>' }}
 {%- if loop.last or (messages[loop.index0 + 1].role != "tool") %}
 {{- '<|im_end|>\n' }}
 {%- endif %}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
 {{- '<|im_start|>assistant\n' }}
 {{- '<think>\n' }}
 {{- 'Let me think through this step by step:\n' }}
 {{- '1. First, I need to understand the question\n' }}
 {{- '2. Then I will work through the solution\n' }}
 {{- '3. Finally, I will provide the answer\n' }}
 {{- '</think>\n' }}
 {{- '\n' }}
 {%- endif %}
)";
            LOGi("Using Qwen3 template with thinking support");
        } else if (chatFormatStr_cpp == "CHATML") {
            // Use ChatML template
            template_content = R"(
{%- if messages[0].role == 'system' %}
{{- '<|im_start|>system\n' + messages[0].content + '<|im_end|>\n' }}
{%- endif %}
{%- for message in messages %}
{%- if message.role != 'system' %}
{{- '<|im_start|>' + message.role + '\n' + message.content + '<|im_end|>\n' }}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
{{- '<|im_start|>assistant\n<think>\n' }}
{%- endif %}
)";
            LOGi("Using ChatML template");
        } else if (chatFormatStr_cpp == "ALPACA") {
            // Use Alpaca template
            template_content = R"(
{%- if messages[0].role == 'system' %}
{{- '### Instruction:\n' + messages[0].content + '\n\n' }}
{%- endif %}
{%- for message in messages %}
{%- if message.role == 'user' %}
{{- '### Input:\n' + message.content + '\n\n' }}
{%- elif message.role == 'assistant' %}
{{- '### Response:\n' + message.content + '\n\n' }}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
{{- '### Response:\n' }}
{%- endif %}
)";
            LOGi("Using Alpaca template");
        } else if (chatFormatStr_cpp == "VICUNA") {
            // Use Vicuna template
            template_content = R"(
{%- if messages[0].role == 'system' %}
{{- messages[0].content + '\n\n' }}
{%- endif %}
{%- for message in messages %}
{%- if message.role == 'user' %}
{{- 'USER: ' + message.content + '\n' }}
{%- elif message.role == 'assistant' %}
{{- 'ASSISTANT: ' + message.content + '\n' }}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
{{- 'ASSISTANT: ' }}
{%- endif %}
)";
            LOGi("Using Vicuna template");
        } else if (chatFormatStr_cpp == "LLAMA2") {
            // Use Llama2 template
            template_content = R"(
{%- if messages[0].role == 'system' %}
{{- '[INST] <<SYS>>\n' + messages[0].content + '\n<</SYS>>\n\n' }}
{%- endif %}
{%- for message in messages %}
{%- if message.role == 'user' %}
{{- message.content + ' [/INST]' }}
{%- elif message.role == 'assistant' %}
{{- ' ' + message.content + ' [INST] ' }}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
{{- ' ' }}
{%- endif %}
)";
            LOGi("Using Llama2 template");
        } else if (chatFormatStr_cpp == "ZEPHYR") {
            // Use Zephyr template
            template_content = R"(
{%- if messages[0].role == 'system' %}
{{- '<|system|>\n' + messages[0].content + '\n<|end|>\n' }}
{%- endif %}
{%- for message in messages %}
{%- if message.role == 'user' %}
{{- '<|user|>\n' + message.content + '\n<|end|>\n' }}
{%- elif message.role == 'assistant' %}
{{- '<|assistant|>\n' + message.content + '\n<|end|>\n' }}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
{{- '<|assistant|>\n' }}
{%- endif %}
)";
            LOGi("Using Zephyr template");
        } else {
            // Default to Qwen3 template for unknown formats
            template_content = R"(
{%- if tools %}
 {{- '<|im_start|>system\n' }}
 {%- if messages[0].role == 'system' %}
 {{- messages[0].content + '\n\n' }}
 {%- endif %}
 {{- "# Tools\n\nYou may call one or more functions to assist with the user query.\n\nYou are provided with function signatures within <tools></tools> XML tags:\n<tools>" }}
 {%- for tool in tools %}
 {{- "\n" }}
 {{- tool | tojson }}
 {%- endfor %}
 {{- "\n</tools>\n\nFor each function call, return a json object with function name and arguments within <tool_call></tool_call> XML tags:\n<tool_call>\n{\"name\": <function-name>, \"arguments\": <args-json-object>}\n</tool_call><|im_end|>\n" }}
{%- else %}
{%- if messages[0].role == 'system' %}
 {{- '<|im_start|>system\n' + messages[0].content + '<|im_end|>\n' }}
 {%- endif %}
{%- endif %}
{%- set ns = namespace(multi_step_tool=true, last_query_index=messages|length - 1) %}
{%- for message in messages[::-1] %}
 {%- set index = (messages|length - 1) - loop.index0 %}
 {%- set tool_start = "<tool_response>" %}
 {%- set tool_start_length = tool_start|length %}
 {%- set start_of_message = message.content[:tool_start_length] %}
 {%- set tool_end = "</tool_response>" %}
 {%- set tool_end_length = tool_end|length %}
 {%- set start_pos = (message.content|length) - tool_end_length %}
 {%- if start_pos < 0 %}
 {%- set start_pos = 0 %}
 {%- endif %}
 {%- set end_of_message = message.content[start_pos:] %}
 {%- if ns.multi_step_tool and message.role == "user" and not(start_of_message == tool_start and end_of_message == tool_end) %}
 {%- set ns.multi_step_tool = false %}
 {%- set ns.last_query_index = index %}
 {%- endif %}
{%- endfor %}
{%- for message in messages %}
 {%- if (message.role == "user") or (message.role == "system" and not loop.first) %}
 {{- '<|im_start|>' + message.role + '\n' + message.content + '<|im_end|>' + '\n' }}
 {%- elif message.role == "assistant" %}
 {%- set content = message.content %}
 {%- set reasoning_content = '' %}
 {%- if message.reasoning_content is defined and message.reasoning_content is not none %}
 {%- set reasoning_content = message.reasoning_content %}
 {%- else %}
 {%- if '</think>' in message.content %}
 {%- set content = (message.content.split('</think>')|last).lstrip('\n') %}
{%- set reasoning_content = (message.content.split('</think>')|first).rstrip('\n') %}
{%- set reasoning_content = (reasoning_content.split('<think>')|last).lstrip('\n') %}
 {%- endif %}
 {%- endif %}
 {%- if loop.index0 > ns.last_query_index %}
 {%- if loop.last or (not loop.last and reasoning_content) %}
 {{- '<|im_start|>' + message.role + '\n<think>\n' + reasoning_content.strip('\n') + '\n</think>\n\n' + content.lstrip('\n') }}
 {%- else %}
 {{- '<|im_start|>' + message.role + '\n' + content }}
 {%- endif %}
 {%- else %}
 {{- '<|im_start|>' + message.role + '\n' + content }}
 {%- endif %}
 {%- if message.tool_calls %}
 {%- for tool_call in message.tool_calls %}
 {%- if (loop.first and content) or (not loop.first) %}
 {{- '\n' }}
 {%- endif %}
 {%- if tool_call.function %}
 {%- set tool_call = tool_call.function %}
 {%- endif %}
 {{- '<tool_call>\n{"name": "' }}
 {{- tool_call.name }}
 {{- '", "arguments": ' }}
 {%- if tool_call.arguments is string %}
 {{- tool_call.arguments }}
 {%- else %}
 {{- tool_call.arguments | tojson }}
 {%- endif %}
 {{- '}\n</tool_call>' }}
 {%- endfor %}
 {%- endif %}
 {{- '<|im_end|>\n' }}
 {%- elif message.role == "tool" %}
 {%- if loop.first or (messages[loop.index0 - 1].role != "tool") %}
 {{- '<|im_start|>user' }}
 {%- endif %}
 {{- '\n<tool_response>\n' }}
 {{- message.content }}
 {{- '\n</tool_response>' }}
 {%- if loop.last or (messages[loop.index0 + 1].role != "tool") %}
 {{- '<|im_end|>\n' }}
 {%- endif %}
{%- endif %}
{%- endfor %}
{%- if add_generation_prompt %}
 {{- '<|im_start|>assistant\n' }}
 {%- if enable_thinking is defined and enable_thinking is false %}
 {{- '<think>\n\n</think>\n\n' }}
 {%- endif %}
{%- endif %}
)";
            LOGi("Using default Qwen3 template for format: %s", chatFormatStr_cpp.c_str());
        }
        
        const auto formattedPrompts = format_chat(model_ptr, template_content, jsonMessages);
        
        LOGi("Template content length: %zu", template_content.length());
        LOGi("Formatted prompt length: %zu", formattedPrompts.length());
        LOGi("Formatted prompt preview: %s", formattedPrompts.substr(0, 200).c_str());

        return env->NewStringUTF(formattedPrompts.c_str());
    } catch (const std::exception &e) {
        LOGe("Error processing data: %s", e.what());
        return env->NewStringUTF("");
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_android_llama_cpp_LLamaAndroid_get_1eot_1str(JNIEnv *env, jobject , jlong jmodel) {
    auto model = reinterpret_cast<llama_model *>(jmodel);
    const auto eot = llama_vocab_eot(llama_model_get_vocab(model));

    if (eot == -1){
        std::string piece = "<|im_end|>";
        return env->NewStringUTF(piece.c_str());
    }

    std::string piece;
    piece.resize(piece.capacity());  // using string internal cache, 15 bytes + '\n'
    const int n_chars = llama_token_to_piece(llama_model_get_vocab(model), eot, &piece[0], piece.size(), 0, true);
    if (n_chars < 0) {
        piece.resize(-n_chars);
        int check = llama_token_to_piece(llama_model_get_vocab(model), eot, &piece[0], piece.size(), 0, true);
        GGML_ASSERT(check == -n_chars);
    }
    else {
        piece.resize(n_chars);
    }

     return env->NewStringUTF(piece.c_str());

}

extern "C" JNIEXPORT jint JNICALL
Java_android_llama_cpp_LLamaAndroid_count_1tokens(JNIEnv *env, jobject, jlong jmodel, jstring jtext) {
    const llama_model *model = reinterpret_cast<llama_model *>(jmodel);
    if (model == nullptr) {
        return 0;
    }
    const char *c_text = env->GetStringUTFChars(jtext, nullptr);
    std::string text(c_text);
    env->ReleaseStringUTFChars(jtext, c_text);

    std::vector<llama_token> tokens(text.size() + 2);
    const struct llama_vocab * vocab = llama_model_get_vocab(model);
    int32_t n_tokens = llama_tokenize(vocab,
                                      text.c_str(),
                                      static_cast<int32_t>(text.size()),
                                      tokens.data(),
                                      static_cast<int32_t>(tokens.size()),
                                      /*add_special*/ false,
                                      /*parse_special*/ true);

    if (n_tokens < 0) n_tokens = 0;
    return n_tokens;
}

extern "C" JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_getMemoryUsageNative(JNIEnv *env, jobject thiz) {
    // Get current memory usage from llama.cpp context
    // For now, return a placeholder value since we need to access the context
    // TODO: Implement proper memory tracking by passing context as parameter
    return 0;
}