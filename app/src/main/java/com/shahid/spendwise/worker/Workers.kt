package com.shahid.spendwise.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shahid.spendwise.R
import com.shahid.spendwise.SpendWiseApplication
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import com.shahid.spendwise.domain.usecase.RecurringScheduleHelper
import com.shahid.spendwise.update.UpdateApiClient
import com.shahid.spendwise.update.VersionComparator
import com.shahid.spendwise.BuildConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

@HiltWorker
class RecurringExpenseWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SpendWiseRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val dueList = repository.getDueRecurring(now)

            for (item in dueList) {
                val transaction = TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amountMinor = item.amountMinor,
                    currency = item.currency,
                    categoryId = item.categoryId,
                    timestampMillis = now,
                    note = "[Recurring] ${item.title}",
                    tags = listOf("recurring", "auto-log"),
                    isTaxRelevant = false,
                    recurringId = item.id
                )
                repository.insertTransaction(transaction)

                val nextDue = RecurringScheduleHelper.advanceUntilFuture(item.nextDueDateMillis, item.frequency, now)
                repository.updateRecurring(item.copy(nextDueDateMillis = nextDue))
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SpendWiseRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val oneDayAhead = now + 24 * 60 * 60 * 1000L
            val recurringList = repository.getAllRecurring().firstOrNull() ?: emptyList()

            val upcomingBills = recurringList.filter {
                it.reminderEnabled && it.nextDueDateMillis in now..oneDayAhead
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            upcomingBills.forEachIndexed { index, bill ->
                val amountStr = Money(bill.amountMinor, bill.currency).format()
                val notification = NotificationCompat.Builder(context, SpendWiseApplication.CHANNEL_REMINDERS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Upcoming Bill Reminder: ${bill.title}")
                    .setContentText("Payment of $amountStr is due within 24 hours.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(3000 + index, notification)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SpendWiseRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek != Calendar.SUNDAY || calendar.get(Calendar.HOUR_OF_DAY) < 18) {
            return Result.success()
        }

        return try {
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L

            val txs = repository.getTransactionsBetween(sevenDaysAgo, now).firstOrNull() ?: emptyList()
            var weeklyExpensesMinor = 0L

            for (tx in txs) {
                if (tx.type == TransactionType.EXPENSE) {
                    weeklyExpensesMinor += tx.amountMinor
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val amountFormatted = Money(weeklyExpensesMinor).format()

            val notification = NotificationCompat.Builder(context, SpendWiseApplication.CHANNEL_WEEKLY)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("SpendWise Weekly Summary")
                .setContentText("You spent a total of $amountFormatted this week. Open SpendWise to view the full category breakdown.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(4001, notification)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiClient: UpdateApiClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = apiClient.getLatestRelease()
            result.onSuccess { release ->
                val hasUpdate = VersionComparator.isUpdateAvailable(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    releaseTag = release.tagName
                )
                if (hasUpdate) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notification = NotificationCompat.Builder(context, SpendWiseApplication.CHANNEL_UPDATES)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("SpendWise Update Available")
                        .setContentText("Version ${release.tagName} is available. Open app to update.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(2002, notification)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
