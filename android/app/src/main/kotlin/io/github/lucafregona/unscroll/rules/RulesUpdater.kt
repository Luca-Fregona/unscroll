package io.github.lucafregona.unscroll.rules

import java.io.IOException

sealed interface RefreshResult {
    val id: String

    data class Updated(override val id: String, val version: String) : RefreshResult
    data class UpToDate(override val id: String) : RefreshResult
    data class Failed(override val id: String, val reason: String) : RefreshResult
}

/** Fetches the bundled rule sets again from [BaseUrl] and hands them to the repository, which validates them. */
class RulesUpdater(private val repo: RulesRepository, private val fetcher: RulesFetcher) {
    fun refresh(base: BaseUrl): List<RefreshResult> = repo.bundledIds().map { refreshOne(base, it) }

    private fun refreshOne(base: BaseUrl, id: String): RefreshResult {
        val body = try {
            fetcher.get(base.fileUrl(id))
        } catch (e: IOException) {
            return RefreshResult.Failed(id, e.message ?: "network error")
        }

        // A file served under the wrong name must not be stored as another rule set.
        val parsed = RuleSetParser.parse(body)
        if (parsed is ParseResult.Ok && parsed.ruleSet.id != id) {
            return RefreshResult.Failed(id, "the file describes \"${parsed.ruleSet.id}\", not \"$id\"")
        }

        val before = repo.current().firstOrNull { it.id == id }?.version
        return when (val stored = repo.import(body)) {
            is ImportResult.Rejected -> RefreshResult.Failed(id, stored.reason)
            is ImportResult.Stored -> {
                val after = repo.current().first { it.id == id }.version
                if (after == before) RefreshResult.UpToDate(id) else RefreshResult.Updated(id, after)
            }
        }
    }
}
