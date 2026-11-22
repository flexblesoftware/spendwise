package com.shahid.spendwise

import com.shahid.spendwise.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyMathTest {

    @Test
    fun testMajorToMinorConversion() {
        val money = Money.fromMajor(19.99, "USD")
        assertEquals(1999L, money.amountMinor)

        val money2 = Money.parseMajor("1,250.50", "USD")
        assertEquals(125050L, money2.amountMinor)
    }

    @Test
    fun testMinorToMajorConversion() {
        val money = Money(1550L, "USD")
        assertEquals(15.50, money.toMajor(), 0.001)
    }

    @Test
    fun testAdditionAndSubtraction() {
        val m1 = Money(5000L, "USD")
        val m2 = Money(2550L, "USD")

        val sum = m1 + m2
        assertEquals(7550L, sum.amountMinor)

        val diff = m1 - m2
        assertEquals(2450L, diff.amountMinor)
    }

    @Test
    fun testCurrencyFormatting() {
        val moneyUsd = Money(10050L, "USD")
        assertEquals("$ 100.50", moneyUsd.format())

        val moneyInr = Money(25000L, "INR")
        assertEquals("₹ 250.00", moneyInr.format())
    }

    @Test
    fun testCurrencyConversionRate() {
        val usdMinor = 10000L
        val inrMinor = Money.convertMinor(usdMinor, 84.50)
        assertEquals(845000L, inrMinor)
    }
}
