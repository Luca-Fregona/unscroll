package io.github.lucafregona.unscroll.rules

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSetParserTest {
    private val valid = """
        {
          "schemaVersion": 1, "id": "example", "name": "Example", "description": "d", "version": "2026.11.04",
          "web": { "matches": ["*://example.com/*"], "redirects": [], "hide": [] },
          "android": { "package": "com.example.app", "testedVersion": "1.0",
            "rules": [ { "id": "r1", "description": "d", "when": { "viewId": "com.example.app:id/x" }, "action": "back" } ] }
        }
    """.trimIndent()

    /** Replaces [old] with [new] in the valid rule set, failing if [old] is not there. */
    private fun mutate(old: String, new: String): String {
        assertTrue("test setup: '$old' not in the template", valid.contains(old))
        return valid.replace(old, new)
    }

    private fun assertInvalid(json: String) {
        val result = RuleSetParser.parse(json)
        assertTrue("expected Invalid but got $result", result is ParseResult.Invalid)
        assertTrue((result as ParseResult.Invalid).reason.isNotBlank())
    }

    @Test
    fun parsesTheSharedFixture() {
        val result = RuleSetParser.parse(File("../../core/fixtures/valid-full.json").readText())
        val ruleSet = (result as ParseResult.Ok).ruleSet
        assertEquals("example", ruleSet.id)
        assertEquals("com.example.app", ruleSet.packageName)
        assertEquals("1.2.3", ruleSet.testedVersion)
        assertEquals(2, ruleSet.rules.size)
        val (plain, tab) = ruleSet.rules
        assertEquals(AndroidRule("feed-screen", "Feed screen", "com.example.app:id/feed_pager", null, false, RuleAction.BACK), plain)
        assertEquals("com.example.app:id/feed_tab", tab.viewId)
        assertTrue(tab.selected)
    }

    @Test
    fun parsesEveryShippedRuleSet() {
        val files = File("../../rules").listFiles { f -> f.isFile && f.name.endsWith(".json") }!!.sortedBy { it.name }
        val shippedWithAndroid = listOf("youtube")
        val parsed = files.associate { it.nameWithoutExtension to RuleSetParser.parse(it.readText()) }
        for ((name, result) in parsed) {
            assertTrue("$name.json: $result", result is ParseResult.Ok || result is ParseResult.NoAndroidBlock)
        }
        for (name in shippedWithAndroid) assertTrue("$name.json must be Ok", parsed[name] is ParseResult.Ok)
    }

    @Test
    fun ignoresTheWebBlockEntirely() {
        val json = mutate(""""web": { "matches": ["*://example.com/*"], "redirects": [], "hide": [] }""", """"web": "not read here"""")
        assertTrue(RuleSetParser.parse(json) is ParseResult.Ok)
    }

    @Test
    fun noAndroidBlock() {
        val webOnly = valid.replace(Regex(""",\s*"android":.*""", RegexOption.DOT_MATCHES_ALL), "\n}")
        assertFalse(webOnly.contains("android"))
        assertEquals(ParseResult.NoAndroidBlock, RuleSetParser.parse(webOnly))
    }

    @Test fun malformedJson() = assertInvalid("{")
    @Test fun notAnObject() = assertInvalid("[1, 2]")
    @Test fun schemaVersionTwo() = assertInvalid(mutate(""""schemaVersion": 1""", """"schemaVersion": 2"""))
    @Test fun idThatIsAPath() = assertInvalid(mutate(""""id": "example"""", """"id": "../x""""))
    @Test fun versionNotADate() = assertInvalid(mutate("2026.11.04", "3 Nov 2026"))
    @Test fun packageWithoutADot() = assertInvalid(mutate("com.example.app\"", "nodots\""))
    @Test fun unknownAction() = assertInvalid(mutate(""""action": "back"""", """"action": "swipe""""))
    @Test fun emptyWhen() = assertInvalid(mutate(""""when": { "viewId": "com.example.app:id/x" }""", """"when": {}"""))
    @Test fun selectedFalse() =
        assertInvalid(mutate("""{ "viewId": "com.example.app:id/x" }""", """{ "viewId": "com.example.app:id/x", "selected": false }"""))
    @Test fun selectedAlone() = assertInvalid(mutate("""{ "viewId": "com.example.app:id/x" }""", """{ "selected": true }"""))
    @Test fun unknownWhenKey() =
        assertInvalid(mutate("""{ "viewId": "com.example.app:id/x" }""", """{ "viewId": "com.example.app:id/x", "enabled": true }"""))
    @Test fun ruleWithoutDescription() = assertInvalid(mutate(""""id": "r1", "description": "d", """, """"id": "r1", """))

    @Test
    fun duplicateRuleIds() {
        val rule = """{ "id": "r1", "description": "d", "when": { "viewId": "com.example.app:id/x" }, "action": "back" }"""
        val json = mutate(rule, "$rule, $rule")
        assertNotEquals(valid, json)
        assertInvalid(json)
    }

    @Test
    fun anAcceptedRuleSetKeepsItsFields() {
        val ruleSet = (RuleSetParser.parse(valid) as ParseResult.Ok).ruleSet
        assertEquals(AndroidRuleSet("example", "2026.11.04", "com.example.app", "1.0", ruleSet.rules), ruleSet)
        assertFalse(ruleSet.rules.single().selected)
    }
}
