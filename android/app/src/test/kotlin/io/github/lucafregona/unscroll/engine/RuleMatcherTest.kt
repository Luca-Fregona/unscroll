package io.github.lucafregona.unscroll.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleMatcherTest {
    private val matcher = RuleMatcher()

    @Test
    fun matchesAViewIdAnywhereInTheTree() {
        val tree = FakeNode(kids = listOf(FakeNode(kids = listOf(FakeNode(kids = listOf(FakeNode(viewId = "app:id/pager")))))))
        val r = rule("r", viewId = "app:id/pager")
        assertEquals(r, matcher.firstMatch(listOf(r), tree))
    }

    @Test
    fun allKeysMustHoldOnTheSameNode() {
        val r = rule("r", viewId = "app:id/tab", contentDescription = "Reels")
        val split = FakeNode(kids = listOf(FakeNode(viewId = "app:id/tab"), FakeNode(contentDescription = "Reels")))
        assertNull(matcher.firstMatch(listOf(r), split))

        val together = FakeNode(kids = listOf(FakeNode(viewId = "app:id/tab", contentDescription = "Reels")))
        assertEquals(r, matcher.firstMatch(listOf(r), together))
    }

    @Test
    fun selectedRequiresASelectedNode() {
        val r = rule("r", viewId = "app:id/tab", selected = true)
        assertNull(matcher.firstMatch(listOf(r), FakeNode(kids = listOf(FakeNode(viewId = "app:id/tab", selected = false)))))
        assertEquals(r, matcher.firstMatch(listOf(r), FakeNode(kids = listOf(FakeNode(viewId = "app:id/tab", selected = true)))))
    }

    @Test
    fun aRuleWithoutSelectedMatchesBothStates() {
        val r = rule("r", viewId = "app:id/tab")
        assertEquals(r, matcher.firstMatch(listOf(r), FakeNode(viewId = "app:id/tab", selected = true)))
        assertEquals(r, matcher.firstMatch(listOf(r), FakeNode(viewId = "app:id/tab", selected = false)))
    }

    @Test
    fun theFirstRuleInListOrderWins() {
        // The node for the second rule comes first in the tree, but the first rule has priority.
        val tree = FakeNode(kids = listOf(FakeNode(viewId = "app:id/second"), FakeNode(viewId = "app:id/first")))
        val first = rule("first", viewId = "app:id/first")
        val second = rule("second", viewId = "app:id/second")
        assertEquals(first, matcher.firstMatch(listOf(first, second), tree))
    }

    @Test
    fun noMatchReturnsNull() {
        val tree = FakeNode(kids = listOf(FakeNode(viewId = "app:id/other")))
        assertNull(matcher.firstMatch(listOf(rule("r", viewId = "app:id/pager")), tree))
        assertNull(matcher.firstMatch(emptyList(), tree))
    }

    @Test
    fun viewIdsMustMatchExactly() {
        val tree = FakeNode(kids = listOf(FakeNode(viewId = "app:id/pager_extra"), FakeNode(viewId = "other:id/pager")))
        assertNull(matcher.firstMatch(listOf(rule("r", viewId = "app:id/pager")), tree))
    }

    @Test
    fun stopsAfterMaxNodes() {
        var reads = 0
        val kids = (0 until 5_000).map { i ->
            FakeNode(viewId = if (i == 4_000) "app:id/pager" else "app:id/n$i", onViewIdRead = { reads++ })
        }
        val tree = FakeNode(kids = kids)
        val result = RuleMatcher(maxNodes = 2_000).firstMatch(listOf(rule("r", viewId = "app:id/pager")), tree)
        assertNull(result)
        assertTrue("looked at $reads nodes", reads in 1..2_000)
    }

    @Test
    fun aNodeTheUserCannotSeeDoesNotMatch() {
        val tree = FakeNode(kids = listOf(FakeNode(viewId = "app:id/pager", visible = false)))
        assertNull(matcher.firstMatch(listOf(rule("r", viewId = "app:id/pager")), tree))
    }

    @Test
    fun aHiddenSubtreeIsNotSearched() {
        val hidden = FakeNode(visible = false, kids = listOf(FakeNode(viewId = "app:id/pager")))
        assertNull(matcher.firstMatch(listOf(rule("r", viewId = "app:id/pager")), FakeNode(kids = listOf(hidden))))
    }

    @Test
    fun doesNotFetchMoreChildrenThanTheBudgetAllows() {
        var fetched = 0
        val root = object : UiNode {
            override val viewId: String? = null
            override val contentDescription: String? = null
            override val selected = false
            override val visible = true
            override val children: Sequence<UiNode> = sequence {
                for (i in 0 until 5_000) {
                    fetched++
                    yield(FakeNode(viewId = "app:id/n$i"))
                }
            }
        }
        RuleMatcher(maxNodes = 100).firstMatch(listOf(rule("r", viewId = "app:id/none")), root)
        assertTrue("fetched $fetched children", fetched <= 100)
    }

    @Test
    fun aMatchInsideTheBudgetIsFound() {
        val kids = (0 until 5_000).map { i -> FakeNode(viewId = if (i == 1_000) "app:id/pager" else "app:id/n$i") }
        val r = rule("r", viewId = "app:id/pager")
        assertEquals(r, RuleMatcher(maxNodes = 2_000).firstMatch(listOf(r), FakeNode(kids = kids)))
    }
}
