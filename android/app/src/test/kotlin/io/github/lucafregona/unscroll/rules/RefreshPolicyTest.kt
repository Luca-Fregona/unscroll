package io.github.lucafregona.unscroll.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    private val hour = 3_600_000L

    @Test fun neverFetchedIsDue() = assertTrue(RefreshPolicy.shouldRefresh(nowMs = 1_000_000, lastMs = 0))
    @Test fun twentyThreeHoursIsNotDue() = assertFalse(RefreshPolicy.shouldRefresh(nowMs = 100 * hour + 23 * hour, lastMs = 100 * hour))
    @Test fun twentyFourHoursIsDue() = assertTrue(RefreshPolicy.shouldRefresh(nowMs = 100 * hour + 24 * hour, lastMs = 100 * hour))
    @Test fun aClockThatMovedBackwardsIsDue() = assertTrue(RefreshPolicy.shouldRefresh(nowMs = 50 * hour, lastMs = 100 * hour))
}
