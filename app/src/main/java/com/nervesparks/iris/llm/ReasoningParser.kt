package com.nervesparks.iris.llm

/**
 * Splits a model response into reasoning/thinking content and final answer.
 * The logic is centralised here so UI and other layers share identical parsing.
 */
object ReasoningParser {
    private val tagRegex = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE)
    private val splitter = Regex("(?:The answer is:|Therefore,|Answer:|Result:)", RegexOption.IGNORE_CASE)

    /**
     * @return Pair(first = reasoning block (may be empty), second = answer)
     */
    fun parse(message: String): Pair<String, String> {
        // Debug logging
        android.util.Log.d("ReasoningParser", "Parsing message: $message")
        
        // First try to find complete <think>...</think> tags
        tagRegex.find(message)?.let { m ->
            val reasoning = m.groupValues[1].trim()
            val answer = message.substring(m.range.last + 1).trim()
            android.util.Log.d("ReasoningParser", "Found complete thinking tags - reasoning: '$reasoning', answer: '$answer'")
            return Pair(reasoning, answer)
        }
        
        // Handle incomplete thinking tags (like when we see </think> but no opening tag)
        if (message.contains("</think>") && !message.contains("<think>")) {
            val parts = message.split("</think>")
            if (parts.size >= 2) {
                val thinkingContent = parts[0].trim()
                val answerContent = parts[1].trim()
                android.util.Log.d("ReasoningParser", "Found incomplete thinking tags - thinking: '$thinkingContent', answer: '$answerContent'")
                return Pair(thinkingContent, answerContent)
            }
        }

        val idx = splitter.find(message)?.range?.first ?: -1
        return if (idx >= 0) {
            val reasoning = message.substring(0, idx).trim()
            val answer = message.substring(idx).replace(splitter, "").trim()
            android.util.Log.d("ReasoningParser", "Found splitter - reasoning: '$reasoning', answer: '$answer'")
            Pair(reasoning, answer)
        } else if (message.contains("Let me think", ignoreCase = true)) {
            android.util.Log.d("ReasoningParser", "Found 'Let me think' - treating as reasoning")
            Pair(message.trim(), "")
        } else {
            android.util.Log.d("ReasoningParser", "No thinking detected - treating as output only")
            Pair("", message.trim())
        }
    }
}