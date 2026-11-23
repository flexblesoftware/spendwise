package com.shahid.spendwise.update

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.shahid.spendwise.BuildConfig
import com.shahid.spendwise.update.model.GitHubAsset
import com.shahid.spendwise.update.model.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpToDate(
        val currentVersionName: String,
        val latestTag: String,
        val checkedAtMillis: Long = System.currentTimeMillis()
    ) : UpdateUiState()
    data class UpdateRequired(
        val release: GitHubRelease,
        val apkAsset: GitHubAsset
    ) : UpdateUiState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
        val tagName: String
    ) : UpdateUiState()
    data class ReadyToInstall(
        val apkFile: File,
        val tagName: String
    ) : UpdateUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val isForcedUpdateError: Boolean = false
    ) : UpdateUiState()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: UpdateApiClient
) {
    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private var activeRelease: GitHubRelease? = null
    private var activeAsset: GitHubAsset? = null

    val hasPendingUpdate: Boolean
        get() = activeRelease != null && activeAsset != null

    suspend fun checkForUpdates(forceCheck: Boolean = false) {
        if (_updateState.value is UpdateUiState.Downloading ||
            _updateState.value is UpdateUiState.ReadyToInstall
        ) {
            return
        }

        _updateState.value = UpdateUiState.Checking

        val result = apiClient.getLatestRelease()
        result.onSuccess { release ->
            val hasUpdate = VersionComparator.isUpdateAvailable(
                currentVersionName = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE,
                releaseTag = release.tagName
            )

            if (hasUpdate) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    activeRelease = release
                    activeAsset = apkAsset

                    // Check if already downloaded and verified
                    val updateDir = File(context.cacheDir, "updates")
                    val targetApk = File(updateDir, "SpendWise-${release.tagName}.apk")
                    if (targetApk.exists() && targetApk.length() == apkAsset.size) {
                        _updateState.value = UpdateUiState.ReadyToInstall(targetApk, release.tagName)
                    } else {
                        _updateState.value = UpdateUiState.UpdateRequired(release, apkAsset)
                    }
                } else {
                    _updateState.value = UpdateUiState.UpToDate(
                        currentVersionName = BuildConfig.VERSION_NAME,
                        latestTag = release.tagName
                    )
                }
            } else {
                _updateState.value = UpdateUiState.UpToDate(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    latestTag = release.tagName
                )
            }
        }.onFailure { error ->
            if (forceCheck) {
                _updateState.value = UpdateUiState.Error(
                    message = error.message ?: "Unable to check for updates. Please check your internet connection.",
                    canRetry = true,
                    isForcedUpdateError = hasPendingUpdate
                )
            } else if (hasPendingUpdate) {
                _updateState.value = UpdateUiState.Error(
                    message = error.message ?: "Unable to complete update check.",
                    canRetry = true,
                    isForcedUpdateError = true
                )
            } else {
                _updateState.value = UpdateUiState.Idle
            }
        }
    }

    fun startDownload() {
        val asset = activeAsset ?: return
        val release = activeRelease ?: return

        // Check if already downloaded and verified
        val updateDir = File(context.cacheDir, "updates")
        val targetApk = File(updateDir, "SpendWise-${release.tagName}.apk")
        if (targetApk.exists() && targetApk.length() == asset.size) {
            _updateState.value = UpdateUiState.ReadyToInstall(targetApk, release.tagName)
            return
        }

        val intent = Intent(context, UpdateDownloadService::class.java).apply {
            putExtra(UpdateDownloadService.EXTRA_DOWNLOAD_URL, asset.downloadUrl)
            putExtra(UpdateDownloadService.EXTRA_FILE_SIZE, asset.size)
            putExtra(UpdateDownloadService.EXTRA_TAG_NAME, release.tagName)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun updateProgress(
        progressPercent: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        tagName: String
    ) {
        _updateState.value = UpdateUiState.Downloading(
            progressPercent = progressPercent,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            tagName = tagName
        )
    }

    fun markDownloadComplete(apkFile: File, tagName: String) {
        _updateState.value = UpdateUiState.ReadyToInstall(apkFile, tagName)
    }

    fun reportDownloadError(errorMessage: String) {
        _updateState.value = UpdateUiState.Error(
            message = errorMessage,
            canRetry = true,
            isForcedUpdateError = true
        )
    }

    fun retry() {
        if (activeAsset != null) {
            startDownload()
        } else {
            _updateState.value = UpdateUiState.Idle
        }
    }
}
