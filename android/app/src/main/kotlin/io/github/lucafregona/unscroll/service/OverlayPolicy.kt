package io.github.lucafregona.unscroll.service

object OverlayPolicy {
    /**
     * The overlay may only stay while a rule still matches and the app in front is one of ours. Events from
     * other apps never reach the service, so leaving the app has to be noticed by looking at what is in front.
     */
    fun shouldStay(matchPresent: Boolean, foregroundPackage: String?, targets: Set<String>): Boolean =
        matchPresent && foregroundPackage != null && foregroundPackage in targets
}
