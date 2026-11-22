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
    }

    @Test
    fun testCalculateVersionCode() {
        val code = VersionComparator.calculateVersionCode("v1.2.3")
        assertEquals(10203, code)

        val code2 = VersionComparator.calculateVersionCode("v2.0.0")
        assertEquals(20000, code2)
    }

    @Test
    fun testIsUpdateAvailable() {
        assertTrue(VersionComparator.isUpdateAvailable("1.0.0", "v1.0.1"))
        assertTrue(VersionComparator.isUpdateAvailable("1.0.0", "v1.1.0"))
        assertTrue(VersionComparator.isUpdateAvailable("1.2.5", "v2.0.0"))
        assertFalse(VersionComparator.isUpdateAvailable("1.0.0", "v1.0.0"))
        assertFalse(VersionComparator.isUpdateAvailable("1.2.0", "v1.1.9"))
    }
}
