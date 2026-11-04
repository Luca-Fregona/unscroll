package io.github.lucafregona.unscroll.rules

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
        // Whatever goes wrong with one file must not take the process down: it runs in the app that hosts the service.
        val body = try {
            fetcher.get(base.fileUrl(id))
        } catch (e: Exception) {
            return RefreshResult.Failed(id, e.message ?: e.javaClass.simpleName)
        }

        // A file served under the wrong name, or for another app, must not end up as the rules of this one.
        val parsed = RuleSetParser.parse(body)
        if (parsed is ParseResult.Ok) {
            if (parsed.ruleSet.id != id) {
                return RefreshResult.Failed(id, "the file describes \"${parsed.ruleSet.id}\", not \"$id\"")
            }
            val expected = repo.bundledPackage(id)
            if (parsed.ruleSet.packageName != expected) {
                return RefreshResult.Failed(id, "the file is for ${parsed.ruleSet.packageName}, not for $expected")
            }
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
