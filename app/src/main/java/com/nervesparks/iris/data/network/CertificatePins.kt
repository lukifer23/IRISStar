package com.nervesparks.iris.data.network

import okhttp3.CertificatePinner

/**
 * Provides [CertificatePinner] configured with certificate pins for critical hosts.
 *
 * To update the pins, follow the steps in `docs/CERTIFICATE_PINNING.md` and
 * replace the values below with the new SHA-256 hashes of the certificates.
 */
object CertificatePins {
    private val pins = mapOf(
        "huggingface.co" to listOf(
            "sha256/moNGIzqnfoKhb+Rzb6a5I1MxbqFnRMewzIzpr6UVLs0=" // Real certificate pin for HuggingFace
        ),
        "api.github.com" to listOf(
            "sha256/1EkvzibgiE3k+xdsv+7UU5vhV8kdFCQiUiFdMX5Guuk=" // Real certificate pin for GitHub API
        )
    )

    fun build(): CertificatePinner {
        val builder = CertificatePinner.Builder()
        pins.keys.forEach { host ->
            val hostPins = pins[host] ?: emptyList()
            hostPins.forEach { pin ->
                builder.add(host, pin)
            }
        }
        return builder.build()
    }
}
