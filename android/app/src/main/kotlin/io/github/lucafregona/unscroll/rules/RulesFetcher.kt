package io.github.lucafregona.unscroll.rules

import java.io.IOException

/** Downloads one rule file. Only the `online` flavor has an implementation. */
interface RulesFetcher {
    /** Returns the response body of a successful GET, and throws [IOException] for anything else. */
    @Throws(IOException::class)
    fun get(url: String): String
}
