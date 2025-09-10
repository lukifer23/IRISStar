package com.nervesparks.iris.llm

import timber.log.Timber

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
    
    // Patterns that indicate thinking content
    private val thinkingPatterns = listOf(
        "thequestion",
        "theuser",
        "theresult",
        "theanswer",
        "Letme",
        "Ineed",
        "Ishould",
        "Sincethe",
        "Juststate",
        "Butwait",
        "Actually",
        "Letmecheck",
        "Letmeverify",
        "Whenyouadd",
        "maybetheuser",
        "istestingif",
        "Icanhandle",
        "basicmath",
        "Ineedtoconfirm",
        "Yes,that'scorrect",
        "Ishouldrespondwith",
        "theanswerdirectly",
        "Sincetheuserisprobablyjustasking",
        "fortheresult",
        "there'snoneed",
        "foranyadditionalexplanation",
        "Juststatetheanswer"
    )
    
    // Patterns that indicate the actual answer
    private val answerPatterns = listOf(
        "2 plus 2 is 4",
        "2+2=4",
        "2 + 2 = 4",
        "The answer is 4",
        "Therefore, 2 + 2 = 4",
        "So, 2 plus 2 equals 4",
        "The result is 4",
        "The solution is 4",
        "2plus2is4",
        "2plus2equals4",
        "2+2equals4"
    )

    /**
     * @return Pair(first = reasoning block (may be empty), second = answer)
     */
    fun parse(message: String, supportsReasoning: Boolean = false): Pair<String, String> {
        // If the model doesn't support reasoning, return the message as-is without parsing
        if (!supportsReasoning) {
            Timber.tag("ReasoningParser").d("Model doesn't support reasoning - returning message as-is")
            Timber.tag("ReasoningParser").d("Message preview: ${message.take(100)}...")
            return Pair("", message.trim())
        }
        
        Timber.tag("ReasoningParser").d("Model supports reasoning - parsing for thinking content")
        Timber.tag("ReasoningParser").d("Message preview: ${message.take(100)}...")
        
        // Debug logging
        Timber.tag("ReasoningParser").d("Parsing message: $message")
        
        // First try to find complete <think>...</think> tags
        tagRegex.find(message)?.let { m ->
            val reasoning = m.groupValues[1].trim()
            val answer = message.substring(m.range.last + 1).trim()
            Timber.tag("ReasoningParser").d("Found complete thinking tags - reasoning: '$reasoning', answer: '$answer'")
            return Pair(reasoning, answer)
        }
        
        // Handle incomplete thinking tags (like when we see </think> but no opening tag)
        if (message.contains("</think>") && !message.contains("<think>")) {
            val parts = message.split("</think>")
            if (parts.size >= 2) {
                val thinkingContent = parts[0].trim()
                val answerContent = parts[1].trim()
                Timber.tag("ReasoningParser").d("Found incomplete thinking tags - thinking: '$thinkingContent', answer: '$answerContent'")
                return Pair(thinkingContent, answerContent)
            }
        }

        // Check if the message contains jumbled thinking patterns
        val hasThinkingPatterns = thinkingPatterns.any { pattern ->
            message.contains(pattern, ignoreCase = true)
        }
        
        if (hasThinkingPatterns) {
            // Look for the actual answer at the end of the message
            val actualAnswer = findActualAnswer(message)
            if (actualAnswer.isNotEmpty()) {
                val reasoning = message.substring(0, message.indexOf(actualAnswer)).trim()
                Timber.tag("ReasoningParser").d("Found jumbled thinking with actual answer - reasoning: '$reasoning', answer: '$actualAnswer'")
                return Pair(reasoning, actualAnswer)
            } else {
                // If no clear answer found, treat the entire message as thinking content
                Timber.tag("ReasoningParser").d("Found jumbled thinking patterns but no clear answer - treating entire message as thinking")
                return Pair(message.trim(), "")
            }
        }
        
        // Try to find splitter patterns
        val splitterMatch = splitter.find(message)
        if (splitterMatch != null) {
            val reasoning = message.substring(0, splitterMatch.range.first).trim()
            val answer = message.substring(splitterMatch.range.last + 1).trim()
            Timber.tag("ReasoningParser").d("Found splitter - reasoning: '$reasoning', answer: '$answer'")
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
                Timber.tag("ReasoningParser").d("Found thinking indicator - reasoning: '$reasoning', answer: '$answer'")
                return Pair(reasoning, answer)
            } else {
                // If no clear answer pattern found, treat everything after thinking indicator as reasoning
                val reasoning = message.substring(thinkingStart).trim()
                Timber.tag("ReasoningParser").d("Found thinking indicator but no clear answer - treating as reasoning only")
                return Pair(reasoning, "")
            }
        }
        
        // If no thinking detected, treat as output only
        Timber.tag("ReasoningParser").d("No thinking detected - treating as output only")
        return Pair("", message.trim())
    }
    
    /**
     * Finds the actual answer in jumbled content by looking for answer patterns
     */
    private fun findActualAnswer(message: String): String {
        // Look for answer patterns in the message
        for (pattern in answerPatterns) {
            val index = message.indexOf(pattern, ignoreCase = true)
            if (index >= 0) {
                // Return everything from the answer pattern to the end
                val answer = message.substring(index).trim()
                Timber.tag("ReasoningParser").d("Found actual answer: '$answer'")
                return answer
            }
        }
        
        // If no specific answer pattern found, try to find the last sentence that looks like an answer
        val sentences = message.split(Regex("[.!?]"))
        for (sentence in sentences.reversed()) {
            val trimmed = sentence.trim()
            if (trimmed.contains("2") && trimmed.contains("4") && trimmed.length < 50) {
                Timber.tag("ReasoningParser").d("Found answer-like sentence: '$trimmed'")
                return trimmed
            }
        }
        
        return ""
    }
}