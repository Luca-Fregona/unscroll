package io.github.lucafregona.unscroll

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledRulesTest {
    private val app = ApplicationProvider.getApplicationContext<UnscrollApp>()

    @Test
    fun theBundledAssetsContainTheShippedAndroidRuleSets() {
        val ids = app.rules.current().map { it.id }
        assertTrue("got $ids", ids.contains("youtube"))
    }

    @Test
    fun theBundledRulesAreForTheOfficialPackages() {
        val youtube = app.rules.current().single { it.id == "youtube" }
        assertEquals("com.google.android.youtube", youtube.packageName)
    }
}
