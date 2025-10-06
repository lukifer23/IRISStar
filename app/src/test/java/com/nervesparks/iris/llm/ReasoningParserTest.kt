package com.nervesparks.iris.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningParserTest {

    @Test
    fun parseHandlesOpenAIStyleThinkTags() {
        val message = """
            <think>
            I should examine the request carefully and recall relevant information.
            I'll outline the important context before producing the answer.
            </think>
            Final answer: Provide the high-level summary requested by the user.
        """.trimIndent()

        val (reasoning, answer) = ReasoningParser.parse(message, supportsReasoning = true)

        assertTrue(reasoning.startsWith("I should examine"))
        assertEquals(
            "Final answer: Provide the high-level summary requested by the user.",
            answer
        )
    }

    @Test
    fun parseHandlesDeepSeekStructuredReasoning() {
        val message = """
            Thoughts:
            ```reasoning
            1. Assess prior examples from the corpus.
            2. Identify the tone that best aligns with the prompt.
            ```
            Answer: Deliver a concise acknowledgement with the selected tone.
        """.trimIndent()

        val (reasoning, answer) = ReasoningParser.parse(message, supportsReasoning = true)

        assertTrue(reasoning.contains("Assess prior examples"))
        assertEquals("Answer: Deliver a concise acknowledgement with the selected tone.", answer)
    }

    @Test
    fun parseHandlesAnthropicStyleReasoningSections() {
        val message = """
            Analysis:
            Step 1. Consider the ethical framing described by the user.
            Step 2. Choose wording that is direct but considerate.

            Final response should reassure the user while acknowledging their concerns.
        """.trimIndent()

        val (reasoning, answer) = ReasoningParser.parse(message, supportsReasoning = true)

        assertTrue(reasoning.contains("Step 1"))
        assertEquals(
            "Final response should reassure the user while acknowledging their concerns.",
            answer
        )
    }
}
