package io.github.lucafregona.unscroll.rules

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RulesRefreshTest {
    @get:Rule val folder = TemporaryFolder()

    private class FakeSettings(override var rulesBaseUrl: String? = null, override var lastRefreshMs: Long = 0) : RefreshSettings

    private class FakeFetcher(val body: (String) -> String = { throw IOException("HTTP 404") }) : RulesFetcher {
        val requested = mutableListOf<String>()
        override fun get(url: String): String {
            requested += url
            return body(url)
        }
    }

    private var now = 100_000_000L
    private val settings = FakeSettings()
    private val repo by lazy {
        RulesRepository(
            listOf(ruleSetJson("youtube", YOUTUBE, version = "2026.11.04"), ruleSetJson("instagram", INSTAGRAM, version = "2026.11.04")),
            File(folder.root, "rules"),
        )
    }
    private val fetcher = FakeFetcher()

    private fun refresher(fetcher: RulesFetcher? = this.fetcher) =
        RulesRefresher(settings, repo, fetcher, "https://example.org/rules", allowLocalCleartext = false, clock = { now })

    @Test
    fun notDueMeansNoFetch() {
        settings.lastRefreshMs = now - 1_000
        assertTrue(refresher().refreshIfDue().isEmpty())
        assertTrue(fetcher.requested.isEmpty())
    }

    @Test
    fun dueMeansOneFetchPerBundledRuleSetAndTheTimeIsRecorded() {
        assertEquals(2, refresher().refreshIfDue().size)
        assertEquals(
            listOf("https://example.org/rules/instagram.json", "https://example.org/rules/youtube.json"),
            fetcher.requested,
        )
        assertEquals(now, settings.lastRefreshMs)
    }

    @Test
    fun theAddressTheUserTypedWinsOverTheDefault() {
        settings.rulesBaseUrl = "https://fork.example.net/my-rules/"
        refresher().refreshNow()
        assertEquals("https://fork.example.net/my-rules/instagram.json", fetcher.requested.first())
    }

    @Test
    fun anInvalidAddressMeansNoFetch() {
        settings.rulesBaseUrl = "http://not-https.example.org"
        assertTrue(refresher().refreshNow().isEmpty())
        assertTrue(fetcher.requested.isEmpty())
    }

    @Test
    fun failuresStillRecordTheTimeAndLeaveTheRulesAlone() {
        val before = repo.current()
        val results = refresher().refreshNow()
        assertTrue(results.all { it is RefreshResult.Failed })
        assertEquals(now, settings.lastRefreshMs)
        assertEquals(before, repo.current())
    }

    @Test
    fun withoutAFetcherNothingHappens() {
        assertTrue(refresher(fetcher = null).refreshIfDue().isEmpty())
        assertEquals(0L, settings.lastRefreshMs)
    }
}
