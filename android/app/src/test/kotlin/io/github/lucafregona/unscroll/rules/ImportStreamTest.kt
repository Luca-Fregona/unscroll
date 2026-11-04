package io.github.lucafregona.unscroll.rules

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportStreamTest {
    @get:Rule val folder = TemporaryFolder()

    private val repo by lazy { RulesRepository(listOf(ruleSetJson("youtube", YOUTUBE)), File(folder.root, "rules")) }

    private fun stream(text: String) = ByteArrayInputStream(text.toByteArray())

    @Test
    fun aValidFileIsImported() {
        val result = repo.importStream(stream(ruleSetJson("youtube", YOUTUBE, version = "2026.12.01")))
        assertEquals(ImportResult.Stored("youtube", "2026.12.01"), result)
    }

    @Test
    fun aFileLargerThanTheLimitIsRejectedWithoutReadingItAll() {
        val endless = object : java.io.InputStream() {
            var read = 0L
            override fun read(): Int {
                read++
                return ' '.code
            }
        }
        val result = repo.importStream(endless)
        assertTrue(result.toString(), result is ImportResult.Rejected)
        assertTrue("read ${endless.read} bytes", endless.read <= MAX_RULES_BYTES + 1L)
    }

    @Test
    fun garbageIsRejected() = assertTrue(repo.importStream(stream("\u0000\u0001 not json")) is ImportResult.Rejected)
}
