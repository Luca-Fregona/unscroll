package io.github.lucafregona.unscroll.rules

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RulesRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val bundled = listOf(
        ruleSetJson("youtube", YOUTUBE, version = "2026.11.04"),
        ruleSetJson("instagram", INSTAGRAM, version = "2026.11.04"),
        webOnlyJson("tiktok"),
    )
    private val store: File by lazy { File(folder.root, "rules") }
    private val repo: RulesRepository by lazy { RulesRepository(bundled, store) }

    private fun plant(fileName: String, text: String) {
        store.mkdirs()
        File(store, fileName).writeText(text)
    }

    private fun RulesRepository.youtube() = current().single { it.id == "youtube" }

    @Test
    fun bundledRuleSetsAreReturnedForBothApps() {
        assertEquals(listOf("instagram", "youtube"), repo.current().map { it.id })
        assertEquals(listOf("instagram", "youtube"), repo.bundledIds())
    }

    @Test
    fun aRuleSetWithoutAnAndroidBlockIsNotAnAndroidRuleSet() {
        assertFalse(repo.current().any { it.id == "tiktok" })
        assertFalse(repo.bundledIds().contains("tiktok"))
    }

    @Test
    fun aNewerStoredRuleSetReplacesTheBundledOne() {
        plant("youtube.json", ruleSetJson("youtube", YOUTUBE, version = "2026.12.01", testedVersion = "stored"))
        assertEquals("stored", repo.youtube().testedVersion)
    }

    @Test
    fun anOlderStoredRuleSetIsIgnored() {
        plant("youtube.json", ruleSetJson("youtube", YOUTUBE, version = "2026.10.01", testedVersion = "stored"))
        assertEquals("bundled", repo.youtube().testedVersion)
    }

    @Test
    fun anEqualStoredRuleSetWinsOverTheBundledOne() {
        plant("youtube.json", ruleSetJson("youtube", YOUTUBE, version = "2026.11.04", testedVersion = "stored"))
        assertEquals("stored", repo.youtube().testedVersion)
    }

    @Test
    fun anInvalidStoredFileIsIgnored() {
        plant("youtube.json", "<html>404</html>")
        assertEquals("bundled", repo.youtube().testedVersion)
    }

    @Test
    fun aStoredFileForAnotherPackageIsIgnored() {
        plant("settings.json", ruleSetJson("settings", "com.android.settings", version = "2030.01.01"))
        plant("youtube.json", ruleSetJson("youtube", "com.android.settings", version = "2030.01.01"))
        assertEquals(listOf("instagram", "youtube"), repo.current().map { it.id })
        assertEquals(YOUTUBE, repo.youtube().packageName)
    }

    @Test
    fun aStoredFileWhoseIdDoesNotMatchItsNameIsIgnored() {
        plant("youtube.json", ruleSetJson("instagram", INSTAGRAM, version = "2030.01.01", testedVersion = "stored"))
        assertEquals("bundled", repo.current().single { it.id == "instagram" }.testedVersion)
    }

    private fun assertRejected(json: String) {
        val result = repo.import(json)
        assertTrue("expected Rejected but got $result", result is ImportResult.Rejected)
        assertFalse("nothing may be stored", store.exists() && store.listFiles()!!.isNotEmpty())
    }

    @Test fun importRejectsInvalidJson() = assertRejected("{")
    @Test fun importRejectsAnHtmlPage() = assertRejected("<!doctype html><html><body>Not found</body></html>")
    @Test fun importRejectsSchemaVersionTwo() =
        assertRejected(ruleSetJson("youtube", YOUTUBE).replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
    @Test fun importRejectsARuleSetWithoutAndroidBlock() = assertRejected(webOnlyJson("youtube"))
    @Test fun importRejectsAnIdThatIsAPath() = assertRejected(ruleSetJson("../evil", YOUTUBE))
    @Test fun importRejectsAnotherPackage() = assertRejected(ruleSetJson("settings", "com.android.settings"))

    @Test
    fun importRejectsAHugeFile() {
        val padding = " ".repeat(MAX_RULES_BYTES)
        assertRejected(ruleSetJson("youtube", YOUTUBE, version = "2030.01.01") + padding)
    }

    @Test
    fun importOfANewerRuleSetStoresItAndTheCacheFollows() {
        assertEquals("bundled", repo.youtube().testedVersion) // fills the cache
        val result = repo.import(ruleSetJson("youtube", YOUTUBE, version = "2026.12.01", testedVersion = "imported"))
        assertEquals(ImportResult.Stored("youtube", "2026.12.01"), result)
        assertEquals("imported", repo.youtube().testedVersion)
        assertEquals(listOf("youtube.json"), store.list()!!.toList())
    }

    @Test
    fun anImportedRuleSetSurvivesANewRepository() {
        repo.import(ruleSetJson("youtube", YOUTUBE, version = "2026.12.01", testedVersion = "imported"))
        assertEquals("imported", RulesRepository(bundled, store).youtube().testedVersion)
    }
}
