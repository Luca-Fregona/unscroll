package io.github.lucafregona.unscroll.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationPacerTest {
    private val pacer = EvaluationPacer(settleMs = 150, minIntervalMs = 500)

    @Test
    fun theFirstEvaluationWaitsOnlyForTheEventsToSettle() = assertEquals(150, pacer.delayUntilNextRun(now = 10_000))

    @Test
    fun anEvaluationRightAfterAnotherOneIsHeldBackToTheMinimumInterval() {
        pacer.markRun(now = 1_000)
        assertEquals(400, pacer.delayUntilNextRun(now = 1_100))
        assertEquals(500, pacer.delayUntilNextRun(now = 1_000))
    }

    @Test
    fun whenTheIntervalIsAlmostOverTheSettleTimeStillApplies() {
        pacer.markRun(now = 1_000)
        assertEquals(150, pacer.delayUntilNextRun(now = 1_450))
    }

    @Test
    fun aLongQuietPeriodIsBackToTheSettleTime() {
        pacer.markRun(now = 1_000)
        assertEquals(150, pacer.delayUntilNextRun(now = 60_000))
    }

    @Test
    fun theWorstCaseIsAtMostAboutTwoWalksASecond() {
        var now = 0L
        var runs = 0
        while (now < 10_000 && runs < 1_000) {
            now += pacer.delayUntilNextRun(now)
            pacer.markRun(now)
            runs++
        }
        assertTrue("$runs walks in ten seconds", runs <= 21)
    }
}
