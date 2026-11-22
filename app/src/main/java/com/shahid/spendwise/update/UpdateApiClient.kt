package com.shahid.spendwise.update

import com.shahid.spendwise.BuildConfig
import com.shahid.spendwise.update.model.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getLatestRelease(
        owner: String = BuildConfig.REPO_OWNER,
        repo: String = BuildConfig.REPO_NAME
    ): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "SpendWise-App")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Failed to fetch release info: HTTP ${response.code}")
                )
            }
            val body = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response body"))

            val release = json.decodeFromString<GitHubRelease>(body)
            Result.success(release)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
