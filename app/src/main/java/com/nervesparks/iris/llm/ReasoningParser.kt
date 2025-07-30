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
        tagRegex.find(message)?.let { m ->
            val reasoning = m.groupValues[1].trim()
            val answer = message.substring(m.range.last + 1).trim()
            return Pair(reasoning, answer)
        }

        val idx = splitter.find(message)?.range?.first ?: -1
        return if (idx >= 0) {
            val reasoning = message.substring(0, idx).trim()
            val answer = message.substring(idx).replace(splitter, "").trim()
            Pair(reasoning, answer)
        } else if (message.contains("Let me think", ignoreCase = true)) {
            Pair(message.trim(), "")
        } else {
            Pair("", message.trim())
        }
    }
}