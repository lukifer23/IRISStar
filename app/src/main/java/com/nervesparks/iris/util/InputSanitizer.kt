package com.nervesparks.iris.util

/** Utility object for sanitizing user-provided strings before use. */
object InputSanitizer {
    /**
     * Basic sanitization to prevent control characters and trim extra whitespace.
     * Additional rules can be added as needed.
     */
    fun sanitize(input: String): String =
        input.replace("[\\p{Cntrl}]".toRegex(), "").trim()
}
