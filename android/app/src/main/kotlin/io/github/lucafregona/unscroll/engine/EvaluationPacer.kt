package io.github.lucafregona.unscroll.engine

/**
 * Decides how long to wait before the next evaluation. A burst of events is evaluated once, after
 * [settleMs], and two evaluations are never closer than [minIntervalMs]: a playing video fires events all
 * the time, and every evaluation walks the target app's node tree.
 */
class EvaluationPacer(private val settleMs: Long = 150, private val minIntervalMs: Long = 500) {
    private var lastRunAt: Long? = null

    fun delayUntilNextRun(now: Long): Long {
        val last = lastRunAt ?: return settleMs
        return maxOf(settleMs, last + minIntervalMs - now)
    }

    fun markRun(now: Long) {
        lastRunAt = now
    }
}
