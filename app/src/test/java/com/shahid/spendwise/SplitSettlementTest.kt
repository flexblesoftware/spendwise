package com.shahid.spendwise

import com.shahid.spendwise.domain.usecase.DebtTransfer
import com.shahid.spendwise.domain.usecase.SplitCalculator
import org.junit.Assert.*
import org.junit.Test

class SplitSettlementTest {

    @Test
    fun testEqualSplitNoRemainder() {
        // $30.00 split among 3 people -> $10.00 each
        val totalMinor = 3000L
        val participantIds = listOf(1L, 2L, 3L)

        val results = SplitCalculator.splitEqually(totalMinor, participantIds)

        assertEquals(3, results.size)
        assertEquals(1000L, results[0].owedAmountMinor)
        assertEquals(1000L, results[1].owedAmountMinor)
        assertEquals(1000L, results[2].owedAmountMinor)
        assertEquals(totalMinor, results.sumOf { it.owedAmountMinor })
    }

    @Test
    fun testEqualSplitWithRemainderPreservesTotal() {
        // $10.00 (1000 minor) split among 3 people -> 334, 333, 333
        val totalMinor = 1000L
        val participantIds = listOf(1L, 2L, 3L)

        val results = SplitCalculator.splitEqually(totalMinor, participantIds)

        assertEquals(3, results.size)
        assertEquals(334L, results[0].owedAmountMinor)
        assertEquals(333L, results[1].owedAmountMinor)
        assertEquals(333L, results[2].owedAmountMinor)
        assertEquals("Total parts must strictly equal total amount", totalMinor, results.sumOf { it.owedAmountMinor })
    }

    @Test
    fun testPercentageSplit() {
        // $100.00 split 50%, 30%, 20%
        val totalMinor = 10000L
        val percentages = mapOf(1L to 50.0, 2L to 30.0, 3L to 20.0)

        val results = SplitCalculator.splitByPercentage(totalMinor, percentages)

        assertEquals(3, results.size)
        val p1 = results.find { it.personId == 1L }?.owedAmountMinor
        val p2 = results.find { it.personId == 2L }?.owedAmountMinor
        val p3 = results.find { it.personId == 3L }?.owedAmountMinor

        assertEquals(5000L, p1)
        assertEquals(3000L, p2)
        assertEquals(2000L, p3)
        assertEquals(totalMinor, results.sumOf { it.owedAmountMinor })
    }

    @Test
    fun testPercentageSplitWithRoundingDrift() {
        // $10.00 (1000 minor) split 33.33%, 33.33%, 33.34%
        val totalMinor = 1000L
        val percentages = mapOf(1L to 33.33, 2L to 33.33, 3L to 33.34)

        val results = SplitCalculator.splitByPercentage(totalMinor, percentages)

        assertEquals("Total parts must equal original amount after drift correction", totalMinor, results.sumOf { it.owedAmountMinor })
    }

    @Test
    fun testSimplifyDebtsTwoParties() {
        // Person 1 is owed $50 (+5000), Person 2 owes $50 (-5000)
        val netBalances = mapOf(
            1L to 5000L,
            2L to -5000L
        )

        val transfers = SplitCalculator.simplifyDebts(netBalances)

        assertEquals(1, transfers.size)
        assertEquals(DebtTransfer(fromPersonId = 2L, toPersonId = 1L, amountMinor = 5000L), transfers[0])
    }

    @Test
    fun testSimplifyDebtsThreePartiesTransitive() {
        // Person 1 paid $60 for Person 2 and Person 3 equally ($20 each).
        // Person 1 net: +$40 (4000)
        // Person 2 net: -$20 (-2000)
        // Person 3 net: -$20 (-2000)
        val netBalances = mapOf(
            1L to 4000L,
            2L to -2000L,
            3L to -2000L
        )

        val transfers = SplitCalculator.simplifyDebts(netBalances)

        assertEquals(2, transfers.size)
        val totalTransferred = transfers.sumOf { it.amountMinor }
        assertEquals(4000L, totalTransferred)
        assertTrue(transfers.all { it.toPersonId == 1L })
    }

    @Test
    fun testSimplifyDebtsBalancedZeroTransfers() {
        // All accounts are settled up
        val netBalances = mapOf(
            1L to 0L,
            2L to 0L,
            3L to 0L
        )

        val transfers = SplitCalculator.simplifyDebts(netBalances)

        assertTrue("No transfers should be needed when all accounts are balanced", transfers.isEmpty())
    }
}
