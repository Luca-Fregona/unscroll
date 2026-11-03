package io.github.lucafregona.unscroll

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import io.github.lucafregona.unscroll.ServiceHarness.device
import io.github.lucafregona.unscroll.ServiceHarness.waitUntil
import io.github.lucafregona.unscroll.ui.SettingsStore
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Turning unscroll off must always be one tap away. */
@RunWith(AndroidJUnit4::class)
class TurnOffTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun enabledServices(): String =
        Settings.Secure.getString(targetContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()

    @Before
    fun setUp() {
        SettingsStore(targetContext).disclosureAccepted = true
        ServiceHarness.enableService()
        device.pressHome()
    }

    @After
    fun tearDown() {
        ServiceHarness.disableService()
    }

    @Test
    fun theTurnOffButtonSwitchesTheServiceOff() {
        assertTrue(enabledServices().contains("UnscrollService"))
        ServiceHarness.launch("io.github.lucafregona.unscroll")
        val button = device.wait(Until.findObject(By.text(targetContext.getString(R.string.turn_off))), 15_000)
        assertTrue("no Turn off button on the main screen", button != null)
        button.click()
        assertTrue("the service is still enabled", waitUntil(8_000) { !enabledServices().contains("UnscrollService") })
    }
}
