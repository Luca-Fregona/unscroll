package io.github.lucafregona.unscroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineBuildTest {
    @Test
    fun onlineDefaults() {
        assertTrue(BuildConfig.ONLINE)
        assertEquals(
            "https://raw.githubusercontent.com/Luca-Fregona/unscroll/main/rules",
            BuildConfig.DEFAULT_RULES_BASE_URL,
        )
    }
}
