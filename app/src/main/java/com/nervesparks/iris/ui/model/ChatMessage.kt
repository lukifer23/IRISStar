package com.nervesparks.iris.ui.model

sealed class ChatMessage(open val content: String) {
    data class User(override val content: String) : ChatMessage(content)
    data class Assistant(override val content: String) : ChatMessage(content)
    data class System(override val content: String) : ChatMessage(content)
    data class Code(override val content: String) : ChatMessage(content)
    data class Reasoning(override val content: String) : ChatMessage(content)
}

