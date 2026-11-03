package io.github.lucafregona.unscroll.rules

import org.junit.Assert.assertNull
import org.junit.Test

class OfflineFlavorRulesTest {
    @Test
    fun theOfflineFlavorCannotFetch() = assertNull(createRulesFetcher())
}
