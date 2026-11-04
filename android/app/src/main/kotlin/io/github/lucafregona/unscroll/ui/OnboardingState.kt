package io.github.lucafregona.unscroll.ui

enum class OnboardingStep { DISCLOSURE, ENABLE_SERVICE, ACTIVE }

/** The disclosure always comes first: nobody is sent to enable the service before reading what it does. */
fun onboardingStep(disclosureAccepted: Boolean, serviceEnabled: Boolean): OnboardingStep = when {
    !disclosureAccepted -> OnboardingStep.DISCLOSURE
    !serviceEnabled -> OnboardingStep.ENABLE_SERVICE
    else -> OnboardingStep.ACTIVE
}

/**
 * From Android 13 the switch of an accessibility service from outside a store is greyed out until the user
 * allows restricted settings. No public API says whether that applies, so the help shows whenever the
 * service is off.
 */
fun needsRestrictedSettingsHelp(sdkInt: Int, serviceEnabled: Boolean): Boolean = sdkInt >= 33 && !serviceEnabled
