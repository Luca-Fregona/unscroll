package io.github.lucafregona.unscroll.ui

import io.github.lucafregona.unscroll.rules.AndroidRuleSet
import io.github.lucafregona.unscroll.rules.INSTAGRAM
import io.github.lucafregona.unscroll.rules.YOUTUBE
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleSummaryTest {
    private fun set(id: String, packageName: String) = AndroidRuleSet(id, "2026.11.04", packageName, "1", emptyList())

    @Test
    fun namesOnlyTheAppsThatHaveRules() =
        assertEquals(listOf("YouTube"), coveredAppNames(listOf(set("youtube", YOUTUBE))))

    @Test
    fun namesEveryAppThatHasRulesInAlphabeticalOrder() =
        assertEquals(listOf("Instagram", "YouTube"), coveredAppNames(listOf(set("youtube", YOUTUBE), set("instagram", INSTAGRAM))))

    @Test
    fun noRulesMeansNoApps() = assertEquals(emptyList<String>(), coveredAppNames(emptyList()))
}
