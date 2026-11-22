package com.shahid.spendwise.update

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shahid.spendwise.R
import com.shahid.spendwise.SpendWiseApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class UpdateDownloadService : Service() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var updateManager: UpdateManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_DOWNLOAD_URL)
        val expectedSize = intent?.getLongExtra(EXTRA_FILE_SIZE, -1L) ?: -1L
        val tagName = intent?.getStringExtra(EXTRA_TAG_NAME) ?: "update"

        if (downloadUrl.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createProgressNotification(0, tagName))
        startDownload(downloadUrl, expectedSize, tagName)

        return START_NOT_STICKY
    }

    private fun startDownload(downloadUrl: String, expectedSize: Long, tagName: String) {
        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            val updateDir = File(cacheDir, "updates").apply { mkdirs() }
            val tempFile = File(updateDir, "SpendWise-${tagName}.apk.part")
            val targetFile = File(updateDir, "SpendWise-${tagName}.apk")

            try {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "SpendWise-App")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed with HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = if (expectedSize > 0) expectedSize else body.contentLength()

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var lastBytesCount = 0L
                var speedBytesPerSec = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            val timeDiff = now - lastProgressUpdate
                            if (timeDiff >= 400) {
                                val progress = if (totalLength > 0) ((downloadedBytes * 100) / totalLength).toInt() else 0
                                val bytesInInterval = downloadedBytes - lastBytesCount
                                speedBytesPerSec = (bytesInInterval * 1000) / timeDiff

                                lastProgressUpdate = now
                                lastBytesCount = downloadedBytes

                                updateNotification(progress, tagName)
                                updateManager.updateProgress(
                                    progressPercent = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalLength,
                                    speedBytesPerSec = speedBytesPerSec,
                                    tagName = tagName
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (expectedSize > 0 && tempFile.length() != expectedSize) {
                    tempFile.delete()
                    throw IllegalStateException(
                        "File size mismatch: expected $expectedSize bytes, received ${tempFile.length()} bytes"
                    )
                }

                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                showCompleteNotification(targetFile, tagName)
                updateManager.markDownloadComplete(targetFile, tagName)
                stopForeground(false)
                stopSelf()

            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                updateManager.reportDownloadError(e.message ?: "Download failed")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun createProgressNotification(progress: Int, tagName: String) =
        NotificationCompat.Builder(this, SpendWiseApplication.CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading SpendWise $tagName")
            .setContentText("$progress% completed")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateNotification(progress: Int, tagName: String) {
        val notification = createProgressNotification(progress, tagName)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompleteNotification(apkFile: File, tagName: String) {
        val installPendingIntent = UpdateInstaller.getInstallPendingIntent(this, apkFile)
        val notification = NotificationCompat.Builder(this, SpendWiseApplication.CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download Complete – SpendWise $tagName")
            .setContentText("Tap to install the latest version")
            .setContentIntent(installPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Install Now",
                installPendingIntent
            )
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_FILE_SIZE = "extra_file_size"
        const val EXTRA_TAG_NAME = "extra_tag_name"
        const val NOTIFICATION_ID = 2001
    }
}
