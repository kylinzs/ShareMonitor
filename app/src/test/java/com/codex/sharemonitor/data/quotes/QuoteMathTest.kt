package com.codex.sharemonitor.data.quotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteMathTest {
    @Test
    fun computeChange_prefersProvided() {
        val result = QuoteMath.computeChange(lastPrice = null, prevClose = null, providedChange = 1.23)
        assertEquals(1.23, requireNotNull(result), 0.0)
    }

    @Test
    fun computeChange_computesFromLastAndPrevClose() {
        val result = QuoteMath.computeChange(lastPrice = 12.0, prevClose = 10.0, providedChange = null)
        assertEquals(2.0, requireNotNull(result), 0.0)
    }

    @Test
    fun computeChange_nullWhenInsufficient() {
        assertNull(QuoteMath.computeChange(lastPrice = 12.0, prevClose = null, providedChange = null))
    }

    @Test
    fun computeChangePct_prefersProvided() {
        val result = QuoteMath.computeChangePct(change = null, prevClose = null, providedChangePct = 9.9)
        assertEquals(9.9, requireNotNull(result), 0.0)
    }

    @Test
    fun computeChangePct_computesFromChangeAndPrevClose() {
        val result = QuoteMath.computeChangePct(change = 1.0, prevClose = 10.0, providedChangePct = null)
        assertEquals(10.0, requireNotNull(result), 1e-9)
    }

    @Test
    fun computeChangePct_nullWhenPrevCloseZero() {
        assertNull(QuoteMath.computeChangePct(change = 1.0, prevClose = 0.0, providedChangePct = null))
    }
}
