package io.github.lucafregona.unscroll

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lucafregona.unscroll.rules.ImportResult
import io.github.lucafregona.unscroll.rules.importStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The file picker hands the repository a stream. This sends one through the same call. */
@RunWith(AndroidJUnit4::class)
class RulesImportTest {
    private val app = ApplicationProvider.getApplicationContext<UnscrollApp>()

    @After
    fun tearDown() = app.rules.resetStored()

    @Test
    fun anImportedNewerRuleSetBecomesTheActiveOne() {
        val json = """
            { "schemaVersion": 1, "id": "youtube", "name": "YouTube", "description": "test", "version": "2099.01.01",
              "android": { "package": "com.google.android.youtube", "testedVersion": "imported", "rules": [
                { "id": "android-test", "description": "test",
                  "when": { "viewId": "com.google.android.youtube:id/reel_recycler" }, "action": "back" } ] } }
        """.trimIndent()
        assertEquals(ImportResult.Stored("youtube", "2099.01.01"), app.rules.importStream(json.byteInputStream()))
        assertEquals("2099.01.01", app.rules.current().single { it.id == "youtube" }.version)
    }
}
