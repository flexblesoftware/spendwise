package com.shahid.spendwise

import com.shahid.spendwise.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

class BudgetMathTest {

    @Test
    fun testBudgetProgressRatio() {
        val budgetLimitMinor = 50000L // $500.00
        val spentMinor = 25000L // $250.00

        val ratio = spentMinor.toFloat() / budgetLimitMinor.toFloat()
        assertEquals(0.5f, ratio, 0.001f)
        assertFalse(ratio >= 0.8f)
        assertFalse(ratio >= 1.0f)
    }

    @Test
    fun test80PercentThresholdTrigger() {
        val budgetLimitMinor = 100000L // $1,000.00
        val spentMinor = 82000L // $820.00

        val ratio = spentMinor.toFloat() / budgetLimitMinor.toFloat()
        assertTrue("Expected >= 80% alert", ratio >= 0.8f)
        assertFalse("Should not be exceeded yet", ratio >= 1.0f)
    }

    @Test
    fun testExceededBudgetAlert() {
        val budgetLimitMinor = 20000L // $200.00
        val spentMinor = 22500L // $225.00

        val ratio = spentMinor.toFloat() / budgetLimitMinor.toFloat()
        assertTrue("Expected >= 80% alert", ratio >= 0.8f)
        assertTrue("Expected exceeded budget alert", ratio >= 1.0f)
    }

    @Test
    fun testMultiCurrencyBudgetAccumulation() {
        // Base currency: USD. Budget = $1,000.00 (100000 minor)
        val budgetLimitMinor = 100000L

        // Transaction 1: $400 USD (40000 minor)
        val tx1Minor = 40000L

        // Transaction 2: €400 EUR (40000 minor) at rate 1.10 = $440.00 (44000 minor)
        val tx2ConvertedMinor = Money.convertMinor(40000L, 1.10)

        // Transaction 3: ₹8,450 INR (845000 minor) at rate 0.01183 = $100.00 (10000 minor)
        val tx3ConvertedMinor = Money.convertMinor(845000L, 0.0118343)

        val totalSpent = tx1Minor + tx2ConvertedMinor + tx3ConvertedMinor
        val ratio = totalSpent.toFloat() / budgetLimitMinor.toFloat()

        assertTrue("Multi-currency total should trigger 80% threshold", ratio >= 0.8f)
        assertFalse("Total should remain under budget limit", ratio >= 1.0f)
    }
}
