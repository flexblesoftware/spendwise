package com.shahid.spendwise

import com.shahid.spendwise.domain.model.RecurrenceFrequency
import com.shahid.spendwise.domain.usecase.RecurringScheduleHelper
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringDateTest {

    @Test
    fun testDailyAdvance() {
        val cal = Calendar.getInstance()
        val startMillis = cal.timeInMillis

        val nextDue = RecurringScheduleHelper.calculateNextDueDate(startMillis, RecurrenceFrequency.DAILY)
        assertTrue(nextDue > startMillis)

        val diffDays = (nextDue - startMillis) / (1000 * 60 * 60 * 24)
        assertTrue(diffDays in 0..1)
    }

    @Test
    fun testAdvanceUntilFuture() {
        val pastMillis = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000L)
        val futureDue = RecurringScheduleHelper.advanceUntilFuture(
            pastMillis,
            RecurrenceFrequency.DAILY,
            System.currentTimeMillis()
        )
        assertTrue(futureDue > System.currentTimeMillis())
    }
}
