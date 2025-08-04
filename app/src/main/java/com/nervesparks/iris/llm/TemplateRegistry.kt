package com.nervesparks.iris.llm

/**
 * Centralised registry for chat prompt templates.
 * Add new templates here and keep the rest of the codebase decoupled from
 * hard-coded format strings.
 */
object TemplateRegistry {

    private const val THINK_OPEN = "<think>"
    private const val THINK_CLOSE = "</think>"

    private val templates: Map<String, (List<Map<String, String>>, String, Boolean) -> String> = mapOf(
        "CHATML" to ::chatMlTemplate,
        "QWEN3" to ::qwen3Template,
        "ALPACA" to ::alpacaTemplate,
        "VICUNA" to ::vicunaTemplate,
        "LLAMA2" to ::llama2Template,
        "ZEPHYR" to ::zephyrTemplate
    )

    /**
     * Render a prompt for the supplied messages.
     *
     * @param templateId   key of template (eg CHATML, QWEN3)
     * @param messages     list of {role, content} maps
     * @param systemPrompt optional system prompt to inject at the top
     * @param includeThinkingTags whether to wrap assistant reasoning with <think>
     */
    fun render(
        templateId: String,
        messages: List<Map<String, String>>,
        systemPrompt: String = "",
        includeThinkingTags: Boolean = true
    ): String {
        val t = templates[templateId.uppercase()] ?: templates.getValue("CHATML")
        return t(messages, systemPrompt, includeThinkingTags)
    }

    // ---------------- private template impls ----------------

    private fun chatMlTemplate(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "system" -> "system"
                "assistant" -> "assistant"
                else -> "user"
            }
            sb.append("<|im_start|>").append(tag).append("\n")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append("<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n") // generation will continue after this
        return sb.toString()
    }

    private fun qwen3Template(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "assistant" -> "assistant"
                "system" -> "system"
                else -> "user"
            }
            sb.append("<|im_start|>").append(tag).append("\n")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append("<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun alpacaTemplate(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("### Instruction:\n").append(systemPrompt).append("\n\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "assistant" -> "### Response:"
                "system" -> "### Instruction:"
                else -> "### Input:"
            }
            sb.append(tag).append("\n")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append("\n\n")
        }
        sb.append("### Response:\n")
        return sb.toString()
    }

    private fun vicunaTemplate(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("SYSTEM: ").append(systemPrompt).append("\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "assistant" -> "ASSISTANT:"
                "system" -> "SYSTEM:"
                else -> "USER:"
            }
            sb.append(tag).append(" ")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append("\n")
        }
        sb.append("ASSISTANT: ")
        return sb.toString()
    }

    private fun llama2Template(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("[INST] <<SYS>>\n").append(systemPrompt).append("\n<</SYS>>\n\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            if (role == "system") continue // system handled above
            val tag = when(role) {
                "assistant" -> "[/INST]"
                else -> "[INST]"
            }
            sb.append(tag).append(" ")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append(" ")
        }
        sb.append("[/INST] ")
        return sb.toString()
    }

    private fun zephyrTemplate(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        includeThinking: Boolean
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) sb.append("<|system|>\n").append(systemPrompt).append("<|end|>\n")
        for (m in messages) {
            val role = m["role"] ?: "user"
            val tag = when(role) {
                "assistant" -> "<|assistant|>"
                "system" -> "<|system|>"
                else -> "<|user|>"
            }
            sb.append(tag).append("\n")
            var content = m["content"] ?: ""
            if (role == "assistant" && includeThinking) {
                content = THINK_OPEN + content + THINK_CLOSE
            }
            sb.append(content).append("<|end|>\n")
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }
}