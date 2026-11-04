package io.github.lucafregona.unscroll

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import io.github.lucafregona.unscroll.ServiceHarness.YOUTUBE
import io.github.lucafregona.unscroll.ServiceHarness.device
import io.github.lucafregona.unscroll.ServiceHarness.waitUntil
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Needs YouTube on the emulator, in English, past its first-run screens. */
@RunWith(AndroidJUnit4::class)
class YoutubeShortsTest {
    private val shortsPlayer = By.res(YOUTUBE, "reel_recycler")
    private val bottomBar = By.res(YOUTUBE, "pivot_bar")

    @Before
    fun setUp() {
        assumeTrue("YouTube is not installed", ServiceHarness.isInstalled(YOUTUBE))
        ServiceHarness.disableService()
        ServiceHarness.forceStop(YOUTUBE)
        device.pressHome()
    }

    @After
    fun tearDown() {
        ServiceHarness.disableService()
        ServiceHarness.forceStop(YOUTUBE)
    }

    private fun openYoutube() {
        ServiceHarness.launch(YOUTUBE)
        assertTrue("YouTube did not show its bottom bar", device.wait(Until.hasObject(bottomBar), 20_000))
    }

    private fun tapShortsTab() {
        val tab = device.findObject(By.desc("Shorts"))
        assertTrue("no Shorts tab in the bottom bar", tab != null)
        tab.click()
    }

    @Test
    fun withoutTheServiceTheShortsTabShowsThePlayer() {
        openYoutube()
        tapShortsTab()
        assertTrue("control run: the Shorts player should show", device.wait(Until.hasObject(shortsPlayer), 15_000))
    }

    @Test
    fun shortsPlayerIsClosed() {
        ServiceHarness.enableService()
        openYoutube()
        tapShortsTab()
        assertTrue(
            "the Shorts player was still showing 8 seconds after the tap",
            waitUntil(8_000) { !device.hasObject(shortsPlayer) && device.hasObject(bottomBar) } ||
                waitUntil(1_000) { device.currentPackageName != YOUTUBE },
        )
        // Give it a moment to prove it stays closed, then check again.
        Thread.sleep(2_500)
        assertFalse("the Shorts player came back", device.hasObject(shortsPlayer))
    }

    @Test
    fun aNormalVideoStaysOpen() {
        ServiceHarness.enableService()
        val videoId = InstrumentationRegistry.getArguments().getString("videoId") ?: "dQw4w9WgXcQ"
        ServiceHarness.open("https://www.youtube.com/watch?v=$videoId", YOUTUBE)
        Thread.sleep(8_000)
        assertTrue("YouTube was closed under a normal video", device.currentPackageName == YOUTUBE)
        assertFalse(device.hasObject(shortsPlayer))
    }
}
