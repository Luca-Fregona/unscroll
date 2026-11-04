package io.github.lucafregona.unscroll.rules

/** What the refresher needs to remember between runs. */
interface RefreshSettings {
    /** The address the user typed, or null for the default. */
    var rulesBaseUrl: String?

    /** Wall-clock time of the last refresh attempt, or 0 when there was none. */
    var lastRefreshMs: Long
}

/**
 * Refreshes the rules from the web when that is possible and due. Without a [fetcher] (the offline flavor) or
 * with an address that is not acceptable it does nothing at all.
 */
class RulesRefresher(
    private val settings: RefreshSettings,
    private val repo: RulesRepository,
    private val fetcher: RulesFetcher?,
    private val defaultBaseUrl: String,
    private val allowLocalCleartext: Boolean,
    private val clock: () -> Long,
) {
    /** The address in use, or null when what the user typed is not acceptable. */
    fun baseUrl(): BaseUrl? = BaseUrl.parse(settings.rulesBaseUrl ?: defaultBaseUrl, allowLocalCleartext)

    fun refreshIfDue(): List<RefreshResult> =
        if (RefreshPolicy.shouldRefresh(clock(), settings.lastRefreshMs)) refreshNow() else emptyList()

    /** Refreshes right away and records the attempt, whether or not every file arrived. */
    fun refreshNow(): List<RefreshResult> {
        val fetcher = fetcher ?: return emptyList()
        val base = baseUrl() ?: return emptyList()
        // The attempt counts even if something blows up, so a failure cannot repeat on every start of the app.
        return try {
            RulesUpdater(repo, fetcher).refresh(base)
        } finally {
            settings.lastRefreshMs = clock()
        }
    }
}
