package com.nervesparks.iris.llm

/**
 * Splits a model response into reasoning/thinking content and final answer.
 * The logic is centralised here so UI and other layers share identical parsing.
 */
object ReasoningParser {
    private val tagRegex = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE)
    private val splitter = Regex("(?:The answer is:|Therefore,|Answer:|Result:|So,|Thus,|Hence,)", RegexOption.IGNORE_CASE)
    private val thinkingIndicators = listOf(
        "Let me think",
        "Let me analyze",
        "Let me consider",
        "I need to think",
        "Let me work through",
        "Let me break this down",
        "Let me figure out",
        "Let me calculate",
        "Let me solve",
        "Let me determine"
    )

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

        // Try to find splitter patterns
        val splitterMatch = splitter.find(message)
        if (splitterMatch != null) {
            val reasoning = message.substring(0, splitterMatch.range.first).trim()
            val answer = message.substring(splitterMatch.range.last + 1).trim()
            android.util.Log.d("ReasoningParser", "Found splitter - reasoning: '$reasoning', answer: '$answer'")
            return Pair(reasoning, answer)
        }
        
        // Check for thinking indicators
        val thinkingIndicator = thinkingIndicators.find { indicator ->
            message.contains(indicator, ignoreCase = true)
        }
        
        if (thinkingIndicator != null) {
            // Find the end of thinking content by looking for common answer patterns
            val answerPatterns = listOf(
                "The answer is",
                "Therefore,",
                "So,",
                "Thus,",
                "Hence,",
                "In conclusion",
                "To answer your question",
                "The result is",
                "The solution is"
            )
            
            val thinkingStart = message.indexOf(thinkingIndicator, ignoreCase = true)
            val answerStart = answerPatterns.mapNotNull { pattern ->
                message.indexOf(pattern, ignoreCase = true).takeIf { it > thinkingStart }
            }.minOrNull()
            
            if (answerStart != null) {
                val reasoning = message.substring(thinkingStart, answerStart).trim()
                val answer = message.substring(answerStart).trim()
                android.util.Log.d("ReasoningParser", "Found thinking indicator - reasoning: '$reasoning', answer: '$answer'")
                return Pair(reasoning, answer)
            } else {
                // If no clear answer pattern found, treat everything after thinking indicator as reasoning
                val reasoning = message.substring(thinkingStart).trim()
                android.util.Log.d("ReasoningParser", "Found thinking indicator but no clear answer - treating as reasoning only")
                return Pair(reasoning, "")
            }
        }
        
        // If no thinking detected, treat as output only
        android.util.Log.d("ReasoningParser", "No thinking detected - treating as output only")
        return Pair("", message.trim())
    }
}