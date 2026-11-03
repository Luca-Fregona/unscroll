package io.github.lucafregona.unscroll.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
    @Test
    fun theDisclosureComesFirstWhateverTheServiceState() {
        assertEquals(OnboardingStep.DISCLOSURE, onboardingStep(disclosureAccepted = false, serviceEnabled = false))
        assertEquals(OnboardingStep.DISCLOSURE, onboardingStep(disclosureAccepted = false, serviceEnabled = true))
    }

    @Test
    fun afterTheDisclosureTheUserIsSentToEnableTheService() =
        assertEquals(OnboardingStep.ENABLE_SERVICE, onboardingStep(disclosureAccepted = true, serviceEnabled = false))

    @Test
    fun onceEnabledTheScreenIsActive() =
        assertEquals(OnboardingStep.ACTIVE, onboardingStep(disclosureAccepted = true, serviceEnabled = true))

    @Test
    fun restrictedSettingsHelpIsForAndroid13AndLaterWhileTheServiceIsOff() {
        assertFalse(needsRestrictedSettingsHelp(sdkInt = 32, serviceEnabled = false))
        assertTrue(needsRestrictedSettingsHelp(sdkInt = 33, serviceEnabled = false))
        assertTrue(needsRestrictedSettingsHelp(sdkInt = 36, serviceEnabled = false))
        assertFalse(needsRestrictedSettingsHelp(sdkInt = 36, serviceEnabled = true))
    }
}
