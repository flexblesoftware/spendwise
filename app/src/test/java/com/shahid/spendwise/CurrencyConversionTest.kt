package com.shahid.spendwise

import com.shahid.spendwise.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConversionTest {

    @Test
    fun testUsdToInrConversion() {
        // $100.00 = 10000 minor units. Rate = 84.50 INR per USD
        val usdMinor = 10000L
        val inrMinor = Money.convertMinor(usdMinor, 84.50)
        assertEquals(845000L, inrMinor) // ₹8,450.00
    }

    @Test
    fun testEurToUsdConversion() {
        // €50.25 = 5025 minor units. Rate = 1.085 USD per EUR
        val eurMinor = 5025L
        val usdMinor = Money.convertMinor(eurMinor, 1.085)
        assertEquals(5452L, usdMinor) // $54.52 (5025 * 1.085 = 5452.125 -> 5452)
    }

    @Test
    fun testJpyConversion() {
        // $15.00 = 1500 minor units. Rate = 155.20 JPY per USD
        val usdMinor = 1500L
        val jpyMinor = Money.convertMinor(usdMinor, 155.20)
        assertEquals(232800L, jpyMinor)
    }

    @Test
    fun testBaseCurrencyIdentityConversion() {
        val amountMinor = 75000L
        val converted = Money.convertMinor(amountMinor, 1.0)
        assertEquals(amountMinor, converted)
    }

    @Test
    fun testRoundingPreservesIntegerPrecision() {
        // 1 minor unit * 1.5 -> 2 minor units (round to nearest)
        assertEquals(2L, Money.convertMinor(1L, 1.5))
        // 1 minor unit * 1.4 -> 1 minor unit
        assertEquals(1L, Money.convertMinor(1L, 1.4))
    }
}
