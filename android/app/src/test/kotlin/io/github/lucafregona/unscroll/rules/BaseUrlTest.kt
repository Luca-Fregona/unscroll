package io.github.lucafregona.unscroll.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlTest {
    private fun parse(input: String, cleartext: Boolean = false) = BaseUrl.parse(input, allowLocalCleartext = cleartext)

    @Test
    fun acceptsHttps() = assertEquals("https://example.org/rules", parse("https://example.org/rules")!!.value)

    @Test
    fun removesTrailingSlashesAndSpaces() = assertEquals("https://example.org/rules", parse("  https://example.org/rules//  ")!!.value)

    @Test
    fun rejectsAnythingThatIsNotHttps() {
        for (input in listOf("http://example.org/rules", "ftp://example.org/rules", "file:///sdcard/rules", "example.org/rules")) {
            assertNull(input, parse(input))
        }
    }

    @Test
    fun rejectsEmptyAndHostlessInput() {
        for (input in listOf("", "   ", "https://", "https:///rules")) assertNull(input, parse(input))
    }

    @Test
    fun rejectsCredentialsQueriesAndFragments() {
        for (input in listOf("https://user:pw@example.org/rules", "https://example.org/rules?x=1", "https://example.org/rules#top")) {
            assertNull(input, parse(input))
        }
    }

    @Test
    fun allowsCleartextOnlyToTheEmulatorHostWhenAsked() {
        assertNull(parse("http://10.0.2.2:8080/rules"))
        assertNotNull(parse("http://10.0.2.2:8080/rules", cleartext = true))
        assertNull(parse("http://example.org/rules", cleartext = true))
    }

    @Test
    fun buildsTheFileUrlOfARuleSet() = assertEquals("https://example.org/rules/youtube.json", parse("https://example.org/rules")!!.fileUrl("youtube"))
}
