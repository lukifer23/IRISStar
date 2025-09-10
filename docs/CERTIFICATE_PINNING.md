# Certificate Pinning

The application pins certificates for critical hosts using OkHttp's [`CertificatePinner`](https://square.github.io/okhttp/features/certificate_pinning/).

## Updating pins
1. Obtain the SHA-256 hash of the host's certificate public key:
   ```bash
   echo | openssl s_client -servername <host> -connect <host>:443 2>/dev/null \
     | openssl x509 -pubkey -noout \
     | openssl pkey -pubin -outform DER \
     | openssl dgst -sha256 -binary \
     | openssl enc -base64
   ```
2. Replace the corresponding entry in `app/src/main/java/com/nervesparks/iris/data/network/CertificatePins.kt` with the new value.
3. Rebuild the app and ensure network requests succeed.

If a certificate rotates, both the new and previous pins can be included temporarily to avoid service disruption.
