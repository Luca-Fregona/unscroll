package io.github.lucafregona.unscroll.engine

import io.github.lucafregona.unscroll.rules.AndroidRuleSet
import io.github.lucafregona.unscroll.rules.RuleAction
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTest {
    private var now = 0L
    private val back = rule("back", viewId = "yt:id/pager")
    private val overlay = rule("overlay", viewId = "yt:id/panel", action = RuleAction.OVERLAY)
    private var ruleSets = listOf(AndroidRuleSet("youtube", "2026.11.04", "com.google.android.youtube", "1", listOf(back, overlay)))
    private val engine = Engine({ ruleSets }, RuleMatcher(), CooldownGate(clock = { now }))

    private val pagerScreen = FakeNode(kids = listOf(FakeNode(viewId = "yt:id/pager")))
    private val panelScreen = FakeNode(kids = listOf(FakeNode(viewId = "yt:id/panel")))
    private val otherScreen = FakeNode(kids = listOf(FakeNode(viewId = "yt:id/home")))

    private object UntouchableNode : UiNode {
        override val viewId: String? get() = error("the tree of another app must not be read")
        override val contentDescription: String? get() = error("the tree of another app must not be read")
        override val selected: Boolean get() = error("the tree of another app must not be read")
        override val visible: Boolean get() = error("the tree of another app must not be read")
        override val children: Sequence<UiNode> get() = error("the tree of another app must not be read")
    }

    @Test
    fun ignoresPackagesWithoutARuleSet() =
        assertEquals(Decision.None, engine.evaluate("com.android.settings", UntouchableNode))

    @Test
    fun noMatchIsNone() = assertEquals(Decision.None, engine.evaluate("com.google.android.youtube", otherScreen))

    @Test
    fun backAndHomeConsumeTheGate() {
        assertEquals(Decision.Match(back, allowed = true), engine.evaluate("com.google.android.youtube", pagerScreen))
        assertEquals(Decision.Match(back, allowed = false), engine.evaluate("com.google.android.youtube", pagerScreen))
        now = 1_000
        assertEquals(Decision.Match(back, allowed = true), engine.evaluate("com.google.android.youtube", pagerScreen))
    }

    @Test
    fun overlayIgnoresTheGate() {
        repeat(5) {
            assertEquals(Decision.Match(overlay, allowed = true), engine.evaluate("com.google.android.youtube", panelScreen))
        }
        assertEquals("the overlay did not use up the gate", Decision.Match(back, allowed = true),
            engine.evaluate("com.google.android.youtube", pagerScreen))
    }

    @Test
    fun ruleSetsAreReadOnEveryCall() {
        ruleSets = emptyList()
        assertEquals(Decision.None, engine.evaluate("com.google.android.youtube", pagerScreen))
        ruleSets = listOf(AndroidRuleSet("youtube", "2026.11.05", "com.google.android.youtube", "1", listOf(back)))
        assertEquals(Decision.Match(back, allowed = true), engine.evaluate("com.google.android.youtube", pagerScreen))
    }
}
