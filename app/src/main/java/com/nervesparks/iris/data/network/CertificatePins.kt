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
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Placeholder - update with real pin
        ),
        "api.github.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Placeholder - update with real pin
        )
    )

    fun build(): CertificatePinner =
        CertificatePinner.Builder().apply {
            pins.forEach { (host, hostPins) ->
                hostPins.forEach { add(host, it) }
            }
        }.build()
}
