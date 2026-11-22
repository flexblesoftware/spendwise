package com.shahid.spendwise

import com.shahid.spendwise.domain.usecase.SplitCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun testEqualSplitWithoutLoss() {
        val participants = listOf(1L, 2L, 3L)
        val result = SplitCalculator.splitEqually(100L, participants)

        assertEquals(3, result.size)
        assertEquals(34L, result[0].owedAmountMinor)
        assertEquals(33L, result[1].owedAmountMinor)
        assertEquals(33L, result[2].owedAmountMinor)

        val total = result.sumOf { it.owedAmountMinor }
        assertEquals(100L, total)
    }

    @Test
    fun testPercentageSplitDriftAdjustment() {
        val percentages = mapOf(1L to 33.33, 2L to 33.33, 3L to 33.34)
        val result = SplitCalculator.splitByPercentage(10000L, percentages)

        val total = result.sumOf { it.owedAmountMinor }
        assertEquals(10000L, total)
    }

    @Test
    fun testSimplifyDebts() {
        val balances = mapOf(
            1L to 5000L,
            2L to -3000L,
            3L to -2000L
        )

        val transfers = SplitCalculator.simplifyDebts(balances)
        assertEquals(2, transfers.size)

        assertEquals(2L, transfers[0].fromPersonId)
        assertEquals(1L, transfers[0].toPersonId)
        assertEquals(3000L, transfers[0].amountMinor)

        assertEquals(3L, transfers[1].fromPersonId)
        assertEquals(1L, transfers[1].toPersonId)
        assertEquals(2000L, transfers[1].amountMinor)
    }
}
