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
        "QWEN3" to ::qwen3Template
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
}