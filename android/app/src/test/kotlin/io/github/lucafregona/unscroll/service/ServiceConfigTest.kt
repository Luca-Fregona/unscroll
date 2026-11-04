package io.github.lucafregona.unscroll.service

import io.github.lucafregona.unscroll.rules.ParseResult
import io.github.lucafregona.unscroll.rules.RuleSetParser
import io.github.lucafregona.unscroll.rules.TargetPackages
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceConfigTest {
    private val config = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(File("src/main/res/xml/accessibility_service_config.xml"))
        .documentElement

    private fun attribute(name: String): String = config.getAttribute("android:$name")

    private fun flags(name: String): Set<String> = attribute(name).split("|").filter { it.isNotBlank() }.toSet()

    @Test
    fun theServiceIsScopedToTheTargetPackagesOnly() {
        assertEquals(TargetPackages.ALL, attribute("packageNames").split(",").map { it.trim() }.toSet())
    }

    @Test
    fun everyPackageWithShippedAndroidRulesIsInScope() {
        val shipped = File("../../rules").listFiles { f -> f.isFile && f.name.endsWith(".json") }!!
            .mapNotNull { (RuleSetParser.parse(it.readText()) as? ParseResult.Ok)?.ruleSet?.packageName }
        assertTrue(shipped.isNotEmpty())
        val inScope = attribute("packageNames").split(",").map { it.trim() }.toSet()
        assertTrue("rules for $shipped but the service only sees $inScope", inScope.containsAll(shipped))
    }

    @Test
    fun theServiceListensToWindowStateAndContentChangesOnly() =
        assertEquals(setOf("typeWindowStateChanged", "typeWindowContentChanged"), flags("accessibilityEventTypes"))

    @Test
    fun theServiceCanReadWindowContentAndViewIds() {
        assertEquals("true", attribute("canRetrieveWindowContent"))
        assertTrue(flags("accessibilityFlags").contains("flagReportViewIds"))
    }

    @Test
    fun theServiceExplainsItselfAndIsNotMarkedAsAnAccessibilityTool() {
        assertEquals("@string/service_description", attribute("description"))
        assertEquals("", attribute("isAccessibilityTool"))
    }
}
