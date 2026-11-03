package io.github.lucafregona.unscroll

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import io.github.lucafregona.unscroll.ServiceHarness.YOUTUBE
import io.github.lucafregona.unscroll.ServiceHarness.device
import io.github.lucafregona.unscroll.ServiceHarness.waitUntil
import io.github.lucafregona.unscroll.rules.ImportResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Same screens as [YoutubeShortsTest], but a throwaway rule set asks for the overlay instead of Back. */
@RunWith(AndroidJUnit4::class)
class OverlayTest {
    private val app = ApplicationProvider.getApplicationContext<UnscrollApp>()
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val shortsPlayer = By.res(YOUTUBE, "reel_recycler")
    private val bottomBar = By.res(YOUTUBE, "pivot_bar")
    private val overlayTitle get() = By.text(targetContext.getString(R.string.overlay_title))
    private val goBack get() = By.text(targetContext.getString(R.string.overlay_back))

    private val overlayRules = """
        { "schemaVersion": 1, "id": "youtube", "name": "YouTube", "description": "test", "version": "2099.01.01",
          "android": { "package": "$YOUTUBE", "testedVersion": "test", "rules": [
            { "id": "android-shorts-overlay", "description": "test",
              "when": { "viewId": "$YOUTUBE:id/reel_recycler" }, "action": "overlay" } ] } }
    """.trimIndent()

    @Before
    fun setUp() {
        assumeTrue("YouTube is not installed", ServiceHarness.isInstalled(YOUTUBE))
        ServiceHarness.disableService()
        ServiceHarness.forceStop(YOUTUBE)
        device.pressHome()
        assertEquals(ImportResult.Stored("youtube", "2099.01.01"), app.rules.import(overlayRules))
    }

    @After
    fun tearDown() {
        ServiceHarness.disableService()
        app.rules.resetStored()
        ServiceHarness.forceStop(YOUTUBE)
    }

    private fun showTheOverlay() {
        ServiceHarness.enableService()
        ServiceHarness.launch(YOUTUBE)
        assertTrue("YouTube did not show its bottom bar", device.wait(Until.hasObject(bottomBar), 20_000))
        device.findObject(By.desc("Shorts")).click()
        assertTrue("the overlay did not show over the Shorts player", device.wait(Until.hasObject(overlayTitle), 15_000))
    }

    @Test
    fun theOverlayGoesAwayWhenTheUserLeavesTheApp() {
        showTheOverlay()
        device.pressHome()
        assertTrue("the overlay was still showing 3 seconds after Home", waitUntil(3_000) { !device.hasObject(overlayTitle) })
        Thread.sleep(2_000)
        assertFalse("the overlay came back over the launcher", device.hasObject(overlayTitle))
    }

    @Test
    fun theGoBackButtonClosesTheOverlayAndLeavesTheShortsPlayer() {
        showTheOverlay()
        device.findObject(goBack).click()
        assertTrue("the overlay was still showing after Go back", waitUntil(3_000) { !device.hasObject(overlayTitle) })
        Thread.sleep(3_000)
        assertFalse("the overlay came back", device.hasObject(overlayTitle))
        assertFalse("the Shorts player is still showing", device.hasObject(shortsPlayer))
    }
}
