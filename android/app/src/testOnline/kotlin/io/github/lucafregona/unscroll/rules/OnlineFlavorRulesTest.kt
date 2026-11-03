package io.github.lucafregona.unscroll.rules

import org.junit.Assert.assertNotNull
import org.junit.Test

class OnlineFlavorRulesTest {
    @Test
    fun theOnlineFlavorCanFetch() = assertNotNull(createRulesFetcher())
}
