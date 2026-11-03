package io.github.lucafregona.unscroll.ui

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lucafregona.unscroll.UnscrollApp
import io.github.lucafregona.unscroll.service.UnscrollService

class MainActivity : ComponentActivity() {
    private lateinit var settings: SettingsStore
    private var disclosureAccepted by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)

    /** Follows the system switch, so the screen is right after the user comes back and after Turn off. */
    private val serviceObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = SettingsStore(this)
        val app = application as UnscrollApp
        refresh()
        setContent {
            UnscrollTheme {
                MainScreen(
                    step = onboardingStep(disclosureAccepted, serviceEnabled),
                    showRestrictedHelp = needsRestrictedSettingsHelp(Build.VERSION.SDK_INT, serviceEnabled),
                    ruleSets = app.rules.current(),
                    onAcceptDisclosure = {
                        settings.disclosureAccepted = true
                        refresh()
                    },
                    onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onOpenAppInfo = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                        )
                    },
                    onTurnOff = ::turnOff,
                    rulesSection = { RulesSourceSection(app) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, serviceObserver,
        )
        refresh()
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(serviceObserver)
        super.onStop()
    }

    private fun refresh() {
        disclosureAccepted = settings.disclosureAccepted
        serviceEnabled = isUnscrollServiceEnabled(this)
    }

    /** The service can switch itself off. When it is not running, the system settings are the way. */
    private fun turnOff() {
        val service = UnscrollService.instance
        if (service != null) service.disableSelf() else startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
