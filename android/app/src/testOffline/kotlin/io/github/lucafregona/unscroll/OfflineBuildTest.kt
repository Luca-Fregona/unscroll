package io.github.lucafregona.unscroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OfflineBuildTest {
    @Test
    fun offlineHasNoUrl() {
        assertFalse(BuildConfig.ONLINE)
        assertEquals("", BuildConfig.DEFAULT_RULES_BASE_URL)
    }
}
