package com.nervesparks.iris.data.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchResultFormatterTest {

    @Test
    fun `formatResults returns message when empty`() {
        val result = SearchResultFormatter.formatResults(emptyList(), "kotlin")
        assertEquals(
            "I couldn't find any relevant information for \"kotlin\". Please try rephrasing your search.",
            result
        )
    }

    @Test
    fun `formatResults formats multiple results`() {
        val results = listOf(
            SearchResult(
                title = "Title1",
                snippet = "Snippet1",
                url = "http://example.com/1",
                source = "source1"
            ),
            SearchResult(
                title = "Title2",
                snippet = "Snippet2",
                url = "http://example.com/2",
                source = "source2"
            )
        )

        val formatted = SearchResultFormatter.formatResults(results, "kotlin")
        val expected = """
            🔍 **Search Results for "kotlin"**
            
            **1. Title1**
            Snippet1
            Source: http://example.com/1
            ---
            
            **2. Title2**
            Snippet2
            Source: http://example.com/2
            ---
            
            These results were found using web search. Please verify any important information.
        """.trimIndent()

        assertEquals(expected, formatted)
    }
}
