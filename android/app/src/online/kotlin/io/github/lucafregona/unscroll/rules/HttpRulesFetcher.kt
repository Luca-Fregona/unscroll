package io.github.lucafregona.unscroll.rules

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A plain HTTP GET with timeouts, no redirects and a size limit. It sends no cookies and no identifiers: its User-Agent is just "unscroll". */
class HttpRulesFetcher : RulesFetcher {
    override fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            // Not the Android default, which carries the OS version and the device model.
            connection.setRequestProperty("User-Agent", "unscroll")
            connection.instanceFollowRedirects = false
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw IOException("HTTP $status")

            val body = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            connection.inputStream.use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    body.write(buffer, 0, read)
                    if (body.size() > MAX_RULES_BYTES) throw IOException("the response is larger than ${MAX_RULES_BYTES / 1024} KiB")
                }
            }
            return body.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
