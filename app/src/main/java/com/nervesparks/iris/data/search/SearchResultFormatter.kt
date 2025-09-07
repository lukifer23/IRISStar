package com.nervesparks.iris.data.search

object SearchResultFormatter {
    fun formatResults(results: List<SearchResult>, query: String): String {
        if (results.isEmpty()) {
            return "I couldn't find any relevant information for \"$query\". Please try rephrasing your search."
        }

        val sb = StringBuilder()
        sb.append("🔍 **Search Results for \"$query\"**\n\n")

        results.forEachIndexed { index, result ->
            sb.append("**${index + 1}. ${result.title}**\n")
            sb.append("${result.snippet}\n")
            sb.append("Source: ${result.url}\n")
            sb.append("---\n\n")
        }

        sb.append("These results were found using web search. Please verify any important information.")
        return sb.toString()
    }
}
