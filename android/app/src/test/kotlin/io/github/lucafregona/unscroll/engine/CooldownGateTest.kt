package io.github.lucafregona.unscroll.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CooldownGateTest {
    private var now = 0L
    private val gate = CooldownGate(clock = { now })

    @Test
    fun theFirstActionIsAllowed() = assertTrue(gate.tryAcquire())

    @Test
    fun aSecondActionNeedsTheMinimumInterval() {
        assertTrue(gate.tryAcquire())
        now = 500
        assertFalse(gate.tryAcquire())
        now = 1_000
        assertTrue(gate.tryAcquire())
    }

    @Test
    fun aRefusedCallDoesNotRestartTheInterval() {
        assertTrue(gate.tryAcquire())
        now = 900
        assertFalse(gate.tryAcquire())
        now = 1_000
        assertTrue(gate.tryAcquire())
    }

    @Test
    fun atMostThreeActionsInTenSeconds() {
        for (t in listOf(0L, 1_000L, 2_000L)) {
            now = t
            assertTrue("action at $t", gate.tryAcquire())
        }
        now = 3_000
        assertFalse(gate.tryAcquire())
        now = 9_999
        assertFalse(gate.tryAcquire())
        now = 10_000
        assertTrue("the first action has left the window", gate.tryAcquire())
    }
}
