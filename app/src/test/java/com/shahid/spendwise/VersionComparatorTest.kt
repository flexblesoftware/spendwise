package com.shahid.spendwise

import com.shahid.spendwise.update.VersionComparator
import org.junit.Assert.*
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testParseSemanticVersion() {
        val (major, minor, patch) = VersionComparator.parseSemanticVersion("v1.2.3")
        assertEquals(1, major)
        assertEquals(2, minor)
        assertEquals(3, patch)

        val (maj2, min2, pat2) = VersionComparator.parseSemanticVersion("2.10.0")
        assertEquals(2, maj2)
        assertEquals(10, min2)
        assertEquals(0, pat2)

        val (maj3, min3, pat3) = VersionComparator.parseSemanticVersion("v1.0.1-rc2")
        assertEquals(1, maj3)
        assertEquals(0, min3)
        assertEquals(1, pat3)
    }

    @Test
    fun testCalculateVersionCode() {
        // v1.2.3 -> 1 * 10000 + 2 * 100 + 3 = 10203
        val code = VersionComparator.calculateVersionCode("v1.2.3")
        assertEquals(10203, code)

        // v1.0.0 -> 10000
        val code1 = VersionComparator.calculateVersionCode("v1.0.0")
        assertEquals(10000, code1)

        // v2.0.0 -> 20000
        val code2 = VersionComparator.calculateVersionCode("v2.0.0")
        assertEquals(20000, code2)
    }

    @Test
    fun testIsUpdateAvailableWithVersionName() {
        // Newer patch
        assertTrue(VersionComparator.isUpdateAvailable(currentVersionName = "1.0.0", releaseTag = "v1.0.1"))

        // Newer minor
        assertTrue(VersionComparator.isUpdateAvailable(currentVersionName = "1.0.0", releaseTag = "v1.1.0"))

        // Newer major
        assertTrue(VersionComparator.isUpdateAvailable(currentVersionName = "1.2.5", releaseTag = "v2.0.0"))

        // Same version
        assertFalse(VersionComparator.isUpdateAvailable(currentVersionName = "1.0.0", releaseTag = "v1.0.0"))

        // Older release
        assertFalse(VersionComparator.isUpdateAvailable(currentVersionName = "1.2.0", releaseTag = "v1.1.9"))
    }

    @Test
    fun testIsUpdateAvailableWithVersionCode() {
        // Current build 10000, release tag v1.0.1 (code 10001) -> update available
        assertTrue(VersionComparator.isUpdateAvailable(currentVersionName = "1.0.0", currentVersionCode = 10000, releaseTag = "v1.0.1"))

        // Current build 10000, release tag v1.0.0 (code 10000) -> false
        assertFalse(VersionComparator.isUpdateAvailable(currentVersionName = "1.0.0", currentVersionCode = 10000, releaseTag = "v1.0.0"))

        // Current build 20000, release tag v1.9.9 (code 10909) -> false
        assertFalse(VersionComparator.isUpdateAvailable(currentVersionName = "2.0.0", currentVersionCode = 20000, releaseTag = "v1.9.9"))
    }
}
