package com.codex.sharemonitor.data.quotes

object QuoteMath {
    fun computeChange(
        lastPrice: Double?,
        prevClose: Double?,
        providedChange: Double?,
    ): Double? {
        if (providedChange != null) return providedChange
        if (lastPrice == null || prevClose == null) return null
        return lastPrice - prevClose
    }

    fun computeChangePct(
        change: Double?,
        prevClose: Double?,
        providedChangePct: Double?,
    ): Double? {
        if (providedChangePct != null) return providedChangePct
        if (change == null || prevClose == null || prevClose == 0.0) return null
        return (change / prevClose) * 100.0
    }
}

