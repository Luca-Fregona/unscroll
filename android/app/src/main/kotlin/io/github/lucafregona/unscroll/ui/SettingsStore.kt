package io.github.lucafregona.unscroll.ui

import android.content.Context
import io.github.lucafregona.unscroll.rules.RefreshSettings

/** Everything the app remembers about the user: whether they read the disclosure, and the rules refresh. */
class SettingsStore(context: Context) : RefreshSettings {
    private val prefs = context.getSharedPreferences("unscroll_settings", Context.MODE_PRIVATE)

    var disclosureAccepted: Boolean
        get() = prefs.getBoolean(DISCLOSURE_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(DISCLOSURE_ACCEPTED, value).apply()

    /** The rules address the user typed, or null for the default. Only the `online` flavor uses it. */
    override var rulesBaseUrl: String?
        get() = prefs.getString(RULES_BASE_URL, null)
        set(value) = prefs.edit().apply { if (value == null) remove(RULES_BASE_URL) else putString(RULES_BASE_URL, value) }.apply()

    /** Wall-clock time of the last rules refresh attempt, or 0 when there was none. */
    override var lastRefreshMs: Long
        get() = prefs.getLong(LAST_REFRESH_MS, 0L)
        set(value) = prefs.edit().putLong(LAST_REFRESH_MS, value).apply()

    private companion object {
        const val DISCLOSURE_ACCEPTED = "disclosure_accepted"
        const val RULES_BASE_URL = "rules_base_url"
        const val LAST_REFRESH_MS = "last_refresh_ms"
    }
}
