package io.github.lucafregona.unscroll.rules

import java.net.URI
import java.net.URISyntaxException

/** Where the `online` flavor looks for rule files: `<value>/<id>.json`. Built only through [parse]. */
class BaseUrl private constructor(val value: String) {
    fun fileUrl(id: String): String = "$value/$id.json"

    override fun toString(): String = value

    companion object {
        /** Hosts a debug build may reach over cleartext: the emulator's alias for the development machine. */
        private val LOCAL_HOSTS = setOf("10.0.2.2", "localhost")

        /** Accepts `https://` URLs without credentials, query or fragment. Returns null for anything else. */
        fun parse(input: String, allowLocalCleartext: Boolean): BaseUrl? {
            val text = input.trim().trimEnd('/')
            val uri = try {
                URI(text)
            } catch (_: URISyntaxException) {
                return null
            }
            val host = uri.host
            if (host.isNullOrEmpty() || uri.userInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
            val acceptable = when (uri.scheme) {
                "https" -> true
                "http" -> allowLocalCleartext && host in LOCAL_HOSTS
                else -> false
            }
            return if (acceptable) BaseUrl(text) else null
        }
    }
}
