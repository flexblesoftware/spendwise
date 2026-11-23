package com.shahid.spendwise

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LauncherIconGenerationTest {

    private val densities = mapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192
    )

    private fun getPngDimensions(file: File): Pair<Int, Int> {
        val bytes = file.readBytes()
        assertTrue("File too small to be valid PNG: ${file.name}", bytes.size >= 24)
        // Verify standard PNG 8-byte magic header
        val expectedHeader = byteArrayOf(
            0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(),
            0x0D, 0x0A, 0x1A, 0x0A
        )
        for (i in expectedHeader.indices) {
            assertEquals("Invalid PNG magic at offset $i in ${file.name}", expectedHeader[i], bytes[i])
        }
        // IHDR chunk: width at 16..19, height at 20..23 (big-endian)
        val width = ((bytes[16].toInt() and 0xFF) shl 24) or
                ((bytes[17].toInt() and 0xFF) shl 16) or
                ((bytes[18].toInt() and 0xFF) shl 8) or
                (bytes[19].toInt() and 0xFF)
        val height = ((bytes[20].toInt() and 0xFF) shl 24) or
                ((bytes[21].toInt() and 0xFF) shl 16) or
                ((bytes[22].toInt() and 0xFF) shl 8) or
                (bytes[23].toInt() and 0xFF)
        return Pair(width, height)
    }

    private fun locateResDir(): File {
        val workingDir = File(".").canonicalFile
        return when {
            File(workingDir, "app/src/main/res").exists() -> File(workingDir, "app/src/main/res")
            File(workingDir, "src/main/res").exists() -> File(workingDir, "src/main/res")
            else -> File(workingDir, "../app/src/main/res")
        }
    }

    private fun locateProjectRoot(): File {
        val workingDir = File(".").canonicalFile
        return if (File(workingDir, "app").exists()) workingDir else workingDir.parentFile
    }

    @Test
    fun testAdaptiveIconVectorResources() {
        val resDir = locateResDir()
        assertTrue("Resource directory must exist: ${resDir.path}", resDir.exists())

        // 1. Background XML check
        val bgFile = File(resDir, "drawable/ic_launcher_background.xml")
        assertTrue("ic_launcher_background.xml must exist", bgFile.exists())
        val bgXml = bgFile.readText()
        assertTrue("Background must use dark green #0B4D3B", bgXml.contains("#0B4D3B") || bgXml.contains("#0b4d3b"))
        assertFalse("Background must NOT have a white stroke border", bgXml.contains("strokeColor=\"#FFFFFF\"") || bgXml.contains("strokeColor=\"#ffffff\""))

        // 2. Foreground XML check
        val fgFile = File(resDir, "drawable/ic_launcher_foreground.xml")
        assertTrue("ic_launcher_foreground.xml must exist", fgFile.exists())
        val fgXml = fgFile.readText()
        assertTrue("Foreground must use bright green #2ECC8F", fgXml.contains("#2ECC8F") || fgXml.contains("#2ecc8f"))
        assertFalse("Foreground must NOT have white border box", fgXml.contains("strokeColor=\"#FFFFFF\"") || fgXml.contains("strokeColor=\"#ffffff\""))
        assertTrue("Canvas viewport must be 108dp", fgXml.contains("viewportWidth=\"108\"") && fgXml.contains("viewportHeight=\"108\""))

        // 3. Monochrome layer check (Android 13+ Material You theming)
        val monoFile = File(resDir, "drawable/ic_launcher_monochrome.xml")
        assertTrue("ic_launcher_monochrome.xml must exist for Android 13+ dynamic theming", monoFile.exists())
        val monoXml = monoFile.readText()
        assertTrue("Monochrome layer must define 108dp viewport", monoXml.contains("viewportWidth=\"108\""))

        // 4. Adaptive launcher XMLs check
        val anyDpiDir = File(resDir, "mipmap-anydpi-v26")
        assertTrue("mipmap-anydpi-v26 must exist", anyDpiDir.exists())

        val launcherXmlFile = File(anyDpiDir, "ic_launcher.xml")
        val launcherRoundXmlFile = File(anyDpiDir, "ic_launcher_round.xml")
        assertTrue("ic_launcher.xml must exist", launcherXmlFile.exists())
        assertTrue("ic_launcher_round.xml must exist", launcherRoundXmlFile.exists())

        val launcherXml = launcherXmlFile.readText()
        val launcherRoundXml = launcherRoundXmlFile.readText()

        assertTrue("ic_launcher.xml must reference background", launcherXml.contains("@drawable/ic_launcher_background"))
        assertTrue("ic_launcher.xml must reference foreground", launcherXml.contains("@drawable/ic_launcher_foreground"))
        assertTrue("ic_launcher.xml must reference monochrome", launcherXml.contains("@drawable/ic_launcher_monochrome"))

        assertTrue("ic_launcher_round.xml must reference background", launcherRoundXml.contains("@drawable/ic_launcher_background"))
        assertTrue("ic_launcher_round.xml must reference foreground", launcherRoundXml.contains("@drawable/ic_launcher_foreground"))
        assertTrue("ic_launcher_round.xml must reference monochrome", launcherRoundXml.contains("@drawable/ic_launcher_monochrome"))
    }

    @Test
    fun testLegacyMipmapRasterIconsExistAndDimensionsValid() {
        val resDir = locateResDir()

        densities.forEach { (density, size) ->
            val dir = File(resDir, "mipmap-$density")
            assertTrue("Density dir mipmap-$density must exist: ${dir.path}", dir.exists())

            // 1. ic_launcher.png (square legacy icon)
            val squareFile = File(dir, "ic_launcher.png")
            assertTrue("Square icon must exist: ${squareFile.path}", squareFile.exists())
            assertTrue("Square icon must not be empty", squareFile.length() > 0)
            val (squareW, squareH) = getPngDimensions(squareFile)
            assertEquals("Square icon width for $density must match", size, squareW)
            assertEquals("Square icon height for $density must match", size, squareH)

            // 2. ic_launcher_round.png (round legacy icon)
            val roundFile = File(dir, "ic_launcher_round.png")
            assertTrue("Round icon must exist: ${roundFile.path}", roundFile.exists())
            assertTrue("Round icon must not be empty", roundFile.length() > 0)
            val (roundW, roundH) = getPngDimensions(roundFile)
            assertEquals("Round icon width for $density must match", size, roundW)
            assertEquals("Round icon height for $density must match", size, roundH)
        }
    }

    @Test
    fun testPlayStoreIcon512x512() {
        val projectRoot = locateProjectRoot()
        val playStoreFile = File(projectRoot, "play_store_512.png")
        val appPlayStoreFile = File(locateResDir().parentFile, "play_store_512.png")

        val targetFile = if (playStoreFile.exists()) playStoreFile else appPlayStoreFile
        assertTrue("Play Store 512x512 icon must exist at ${targetFile.path}", targetFile.exists())
        assertTrue("Play Store 512x512 icon must not be empty", targetFile.length() > 0)

        val (w, h) = getPngDimensions(targetFile)
        assertEquals("Play Store icon width must be 512", 512, w)
        assertEquals("Play Store icon height must be 512", 512, h)
    }
}
