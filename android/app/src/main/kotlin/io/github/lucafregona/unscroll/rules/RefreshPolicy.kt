package io.github.lucafregona.unscroll.rules

object RefreshPolicy {
    /** True when rules were never fetched, the interval has passed, or the clock went backwards. */
    fun shouldRefresh(nowMs: Long, lastMs: Long, intervalMs: Long = 86_400_000): Boolean =
        lastMs == 0L || nowMs < lastMs || nowMs - lastMs >= intervalMs
}
