package com.nervesparks.iris

data class Template(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val content: String
)
