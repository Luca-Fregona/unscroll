package io.github.lucafregona.unscroll.rules

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RulesUpdaterTest {
    @get:Rule val folder = TemporaryFolder()

    private class FakeFetcher(var responses: Map<String, () -> String>) : RulesFetcher {
        val requested = mutableListOf<String>()
        override fun get(url: String): String {
            requested += url
            return (responses[url] ?: throw IOException("HTTP 404"))()
        }
    }

    private val base = BaseUrl.parse("https://example.org/rules", allowLocalCleartext = false)!!
    private val repo by lazy {
        RulesRepository(
            listOf(
                ruleSetJson("youtube", YOUTUBE, version = "2026.11.04"),
                ruleSetJson("instagram", INSTAGRAM, version = "2026.11.04"),
            ),
            File(folder.root, "rules"),
        )
    }
    private val fetcher = FakeFetcher(emptyMap())
    private val updater by lazy { RulesUpdater(repo, fetcher) }

    private fun serve(id: String, body: String) {
        fetcher.responses += base.fileUrl(id) to { body }
    }

    @Test
    fun requestsOneFilePerBundledRuleSet() {
        updater.refresh(base)
        assertEquals(listOf("https://example.org/rules/instagram.json", "https://example.org/rules/youtube.json"), fetcher.requested)
    }

    @Test
    fun aNewerVersionIsUpdated() {
        serve("youtube", ruleSetJson("youtube", YOUTUBE, version = "2026.12.01", testedVersion = "fetched"))
        val result = updater.refresh(base).single { it.id == "youtube" }
        assertEquals(RefreshResult.Updated("youtube", "2026.12.01"), result)
        assertEquals("fetched", repo.current().single { it.id == "youtube" }.testedVersion)
    }

    @Test
    fun theSameVersionIsUpToDate() {
        serve("youtube", ruleSetJson("youtube", YOUTUBE, version = "2026.11.04", testedVersion = "fetched"))
        assertEquals(RefreshResult.UpToDate("youtube"), updater.refresh(base).single { it.id == "youtube" })
    }

    @Test
    fun anOlderVersionChangesNothing() {
        serve("youtube", ruleSetJson("youtube", YOUTUBE, version = "2026.10.01", testedVersion = "fetched"))
        assertEquals(RefreshResult.UpToDate("youtube"), updater.refresh(base).single { it.id == "youtube" })
        assertEquals("bundled", repo.current().single { it.id == "youtube" }.testedVersion)
    }

    @Test
    fun aNetworkErrorFails() {
        fetcher.responses += base.fileUrl("youtube") to { throw IOException("timeout") }
        assertEquals(RefreshResult.Failed("youtube", "timeout"), updater.refresh(base).single { it.id == "youtube" })
    }

    @Test
    fun aFileForAnotherIdFails() {
        serve("youtube", ruleSetJson("instagram", INSTAGRAM, version = "2030.01.01", testedVersion = "fetched"))
        val result = updater.refresh(base).single { it.id == "youtube" }
        assertTrue(result.toString(), result is RefreshResult.Failed)
        assertEquals("bundled", repo.current().single { it.id == "instagram" }.testedVersion)
    }

    @Test
    fun invalidContentFails() {
        serve("youtube", "<html>Not found</html>")
        assertTrue(updater.refresh(base).single { it.id == "youtube" } is RefreshResult.Failed)
    }

    @Test
    fun oneFailureDoesNotStopTheOthers() {
        fetcher.responses += base.fileUrl("instagram") to { throw IOException("boom") }
        serve("youtube", ruleSetJson("youtube", YOUTUBE, version = "2026.12.01", testedVersion = "fetched"))
        val results = updater.refresh(base).associateBy { it.id }
        assertTrue(results.getValue("instagram") is RefreshResult.Failed)
        assertTrue(results.getValue("youtube") is RefreshResult.Updated)
    }

    @Test
    fun activeRulesAreUnchangedAfterEveryFailure() {
        val before = repo.current()
        serve("youtube", "{")
        serve("instagram", ruleSetJson("instagram", "com.android.settings", version = "2030.01.01"))
        updater.refresh(base)
        assertEquals(before, repo.current())
    }
}
