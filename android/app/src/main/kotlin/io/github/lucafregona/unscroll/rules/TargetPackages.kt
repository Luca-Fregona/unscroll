package io.github.lucafregona.unscroll.rules

/**
 * The only apps unscroll acts in. This set must equal `packageNames` in
 * `res/xml/accessibility_service_config.xml`, which a unit test checks.
 */
object TargetPackages {
    val ALL: Set<String> = setOf("com.instagram.android", "com.google.android.youtube")
}
