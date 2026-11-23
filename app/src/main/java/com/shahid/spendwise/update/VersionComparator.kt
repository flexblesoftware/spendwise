package com.shahid.spendwise.update

object VersionComparator {

    /**
     * Parses semantic version from tag strings like "v1.2.3" or "1.2.3".
     * Returns Triple(major, minor, patch).
     */
    fun parseSemanticVersion(tag: String): Triple<Int, Int, Int> {
        val clean = tag.trim().removePrefix("v").substringBefore("-")
        val parts = clean.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    /**
     * Calculates versionCode according to formula:
     * versionCode = major * 10000 + minor * 100 + patch
     */
    fun calculateVersionCode(tag: String): Int {
        val (major, minor, patch) = parseSemanticVersion(tag)
        return major * 10000 + minor * 100 + patch
    }

    /**
     * Checks if releaseTag represents a newer version than currentVersionName / currentVersionCode.
     */
    fun isUpdateAvailable(
        currentVersionName: String,
        currentVersionCode: Int = 0,
        releaseTag: String
    ): Boolean {
        val releaseCode = calculateVersionCode(releaseTag)
        if (currentVersionCode > 0 && releaseCode > currentVersionCode) {
            return true
        }

        val current = parseSemanticVersion(currentVersionName)
        val release = parseSemanticVersion(releaseTag)

        if (release.first > current.first) return true
        if (release.first < current.first) return false

        if (release.second > current.second) return true
        if (release.second < current.second) return false

        return release.third > current.third
    }
}
