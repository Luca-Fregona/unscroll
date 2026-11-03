package io.github.lucafregona.unscroll.service

import io.github.lucafregona.unscroll.rules.TargetPackages
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPolicyTest {
    private val targets = TargetPackages.ALL

    @Test
    fun staysWhileAMatchIsPresentInATargetApp() =
        assertTrue(OverlayPolicy.shouldStay(matchPresent = true, foregroundPackage = "com.google.android.youtube", targets = targets))

    @Test
    fun goesWhenThereIsNoMatchAnymore() =
        assertFalse(OverlayPolicy.shouldStay(matchPresent = false, foregroundPackage = "com.google.android.youtube", targets = targets))

    @Test
    fun goesWhenTheUserLeftTheApp() {
        assertFalse(OverlayPolicy.shouldStay(matchPresent = true, foregroundPackage = "com.google.android.apps.nexuslauncher", targets = targets))
        assertFalse(OverlayPolicy.shouldStay(matchPresent = true, foregroundPackage = null, targets = targets))
    }
}
