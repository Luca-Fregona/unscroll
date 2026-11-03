package io.github.lucafregona.unscroll.engine

/**
 * Keeps Back and Home presses from turning into a storm: at least [minIntervalMs] between two actions and
 * at most [maxActions] inside any [windowMs]. A refused call is not recorded.
 */
class CooldownGate(
    private val minIntervalMs: Long = 1_000,
    private val maxActions: Int = 3,
    private val windowMs: Long = 10_000,
    private val clock: () -> Long,
) {
    private val actions = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = clock()
        while (actions.isNotEmpty() && now - actions.first() >= windowMs) actions.removeFirst()
        val last = actions.lastOrNull()
        if (last != null && now - last < minIntervalMs) return false
        if (actions.size >= maxActions) return false
        actions.addLast(now)
        return true
    }
}
