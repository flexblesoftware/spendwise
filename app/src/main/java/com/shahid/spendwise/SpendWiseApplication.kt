package com.shahid.spendwise

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shahid.spendwise.worker.BillReminderWorker
import com.shahid.spendwise.worker.RecurringExpenseWorker
import com.shahid.spendwise.worker.UpdateCheckWorker
import com.shahid.spendwise.worker.WeeklySummaryWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SpendWiseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleBackgroundWorkers()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_reminders_desc)
                enableVibration(true)
            }

            val updateChannel = NotificationChannel(
                CHANNEL_UPDATES,
                getString(R.string.notification_channel_updates),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_updates_desc)
                setSound(null, null)
            }

            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY,
                getString(R.string.notification_channel_weekly),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_weekly_desc)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, updateChannel, weeklyChannel))
        }
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        val recurringWork = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "SpendWiseRecurringSync",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWork
        )

        val billReminderWork = PeriodicWorkRequestBuilder<BillReminderWorker>(12, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "SpendWiseBillReminders",
            ExistingPeriodicWorkPolicy.KEEP,
            billReminderWork
        )

        val weeklySummaryWork = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "SpendWiseWeeklySummary",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklySummaryWork
        )

        val updateConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val updateWork = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(updateConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "SpendWisePeriodicUpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWork
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "spendwise_reminders"
        const val CHANNEL_UPDATES = "spendwise_updates"
        const val CHANNEL_WEEKLY = "spendwise_weekly"
    }
}
