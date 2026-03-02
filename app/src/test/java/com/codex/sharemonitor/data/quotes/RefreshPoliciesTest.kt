package com.codex.sharemonitor.data.quotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPoliciesTest {
    @Test
    fun shouldThrottle_respectsWindow() {
        assertTrue(
            RefreshPolicies.shouldThrottle(
                lastAttemptEpochMillis = 1_000,
                nowEpochMillis = 2_000,
                throttleWindowMs = 1_500,
                force = false,
            )
        )
        assertFalse(
            RefreshPolicies.shouldThrottle(
                lastAttemptEpochMillis = 1_000,
                nowEpochMillis = 2_000,
                throttleWindowMs = 500,
                force = false,
            )
        )
        assertFalse(
            RefreshPolicies.shouldThrottle(
                lastAttemptEpochMillis = 1_000,
                nowEpochMillis = 2_000,
                throttleWindowMs = 1_500,
                force = true,
            )
        )
    }

    @Test
    fun remainingIntervalMs_returnsNullWhenAllowed() {
        assertNull(
            RefreshPolicies.remainingIntervalMs(
                lastSuccessEpochMillis = 1_000,
                nowEpochMillis = 10_000,
                minIntervalMs = 5_000,
                force = false,
            )
        )
    }

    @Test
    fun remainingIntervalMs_returnsRemainingWhenWithinInterval() {
        val remaining = RefreshPolicies.remainingIntervalMs(
            lastSuccessEpochMillis = 1_000,
            nowEpochMillis = 2_000,
            minIntervalMs = 5_000,
            force = false,
        )
        assertNotNull(remaining)
        assertEquals(4_000L, remaining)
        assertEquals("距离下次更新还有 1 分钟", RefreshPolicies.remainingMessage(remaining!!))
    }

    @Test
    fun isStale_trueWhenBeyondThreshold() {
        assertTrue(RefreshPolicies.isStale(fetchedAtEpochMillis = 1_000, nowEpochMillis = 10_001, staleAfterMs = 9_000))
        assertFalse(RefreshPolicies.isStale(fetchedAtEpochMillis = 1_000, nowEpochMillis = 10_000, staleAfterMs = 9_000))
    }
}
