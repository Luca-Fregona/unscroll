package io.github.lucafregona.unscroll.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetPackagesTest {
    @Test
    fun everyTargetPackageHasAReadableName() {
        assertEquals("YouTube", TargetPackages.displayName("com.google.android.youtube"))
        assertEquals("Instagram", TargetPackages.displayName("com.instagram.android"))
        assertEquals(TargetPackages.ALL, TargetPackages.ALL.filter { TargetPackages.displayName(it) != it }.toSet())
    }

    @Test
    fun anUnknownPackageIsShownAsItsPackageName() =
        assertEquals("com.example.other", TargetPackages.displayName("com.example.other"))
}
