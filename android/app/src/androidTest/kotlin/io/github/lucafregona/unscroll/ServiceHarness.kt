package io.github.lucafregona.unscroll

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import io.github.lucafregona.unscroll.service.UnscrollService

/** Drives the real service and the real apps on the emulator. Needs a device with those apps installed. */
object ServiceHarness {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"
    private const val COMPONENT = "io.github.lucafregona.unscroll/.service.UnscrollService"

    init {
        // While UiAutomation is connected, Android does not bind the other accessibility services, ours included.
        Configurator.getInstance().uiAutomationFlags = UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
    }

    val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Writes a secure setting through the shell identity of UiAutomation, the way `adb shell` would. */
    private fun setting(key: String, value: String) {
        device.executeShellCommand("settings put secure $key $value")
    }

    fun enableService() {
        setting("enabled_accessibility_services", COMPONENT)
        setting("accessibility_enabled", "1")
        check(waitUntil(10_000) { UnscrollService.instance != null }) { "the service did not connect" }
    }

    fun disableService() {
        setting("enabled_accessibility_services", "\"\"")
        check(waitUntil(10_000) { UnscrollService.instance == null }) { "the service did not disconnect" }
    }

    /** Starts the app's launcher activity. The shell does not depend on package visibility rules. */
    fun launch(packageName: String) {
        device.executeShellCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
    }

    fun open(url: String, packageName: String) {
        device.executeShellCommand("am start -a android.intent.action.VIEW -d $url $packageName")
    }

    fun isInstalled(packageName: String): Boolean = device.executeShellCommand("pm path $packageName").isNotBlank()

    fun forceStop(packageName: String) {
        device.executeShellCommand("am force-stop $packageName")
    }

    fun waitUntil(timeoutMs: Long, check: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (check()) return true
            Thread.sleep(150)
        }
        return check()
    }
}
