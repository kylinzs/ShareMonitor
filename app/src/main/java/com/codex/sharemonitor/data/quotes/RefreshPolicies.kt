package com.codex.sharemonitor.data.quotes

import kotlin.math.ceil

object RefreshPolicies {
    fun shouldThrottle(
        lastAttemptEpochMillis: Long,
        nowEpochMillis: Long,
        throttleWindowMs: Long,
        force: Boolean,
    ): Boolean {
        if (force) return false
        if (lastAttemptEpochMillis <= 0) return false
        return (nowEpochMillis - lastAttemptEpochMillis) < throttleWindowMs
    }

    fun remainingIntervalMs(
        lastSuccessEpochMillis: Long,
        nowEpochMillis: Long,
        minIntervalMs: Long,
        force: Boolean,
    ): Long? {
        if (force) return null
        if (lastSuccessEpochMillis <= 0) return null
        val elapsed = nowEpochMillis - lastSuccessEpochMillis
        if (elapsed >= minIntervalMs) return null
        return minIntervalMs - elapsed
    }

    fun isStale(
        fetchedAtEpochMillis: Long,
        nowEpochMillis: Long,
        staleAfterMs: Long,
    ): Boolean {
        return (nowEpochMillis - fetchedAtEpochMillis) > staleAfterMs
    }

    fun remainingMessage(remainingMs: Long): String {
        val minutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
        return "距离下次更新还有 $minutes 分钟"
    }
}

