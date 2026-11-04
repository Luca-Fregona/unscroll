package io.github.lucafregona.unscroll.rules

/**
 * The only apps unscroll acts in. This set must equal `packageNames` in
 * `res/xml/accessibility_service_config.xml`, which a unit test checks.
 */
object TargetPackages {
    private val NAMES = mapOf(
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
    )

    val ALL: Set<String> = NAMES.keys

    /** The name people know the app by, or the package name for anything unknown. */
    fun displayName(packageName: String): String = NAMES[packageName] ?: packageName
}
