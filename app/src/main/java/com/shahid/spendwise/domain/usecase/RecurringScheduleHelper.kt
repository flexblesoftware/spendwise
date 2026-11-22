package com.shahid.spendwise.domain.usecase

import com.shahid.spendwise.domain.model.RecurrenceFrequency
import java.util.Calendar

object RecurringScheduleHelper {

    fun calculateNextDueDate(
        baseMillis: Long,
        frequency: RecurrenceFrequency
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseMillis
        }
        when (frequency) {
            RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun advanceUntilFuture(
        dueMillis: Long,
        frequency: RecurrenceFrequency,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        var current = dueMillis
        while (current <= nowMillis) {
            current = calculateNextDueDate(current, frequency)
        }
        return current
    }
}
