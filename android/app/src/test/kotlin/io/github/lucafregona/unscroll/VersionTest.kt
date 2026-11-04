package io.github.lucafregona.unscroll

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionTest {
    @Test
    fun versionNameEqualsPackageJson() {
        val text = File("../../package.json").readText()
        val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(text)!!.groupValues[1]
        assertEquals(version, BuildConfig.VERSION_NAME)
    }
}
