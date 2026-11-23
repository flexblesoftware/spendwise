package com.shahid.spendwise

import org.junit.Assert.*
import org.junit.Test
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class LauncherIconGenerationTest {

    private val bgDarkGreen = Color(0x0B, 0x4D, 0x3B)
    private val fgBrightGreen = Color(0x2E, 0xCC, 0x8F)

    private val densities = mapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192
    )

    private fun renderIcon(size: Int, isRound: Boolean): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        // 1. Draw Background: full-bleed solid dark green (or circular for round icon)
        g2.color = bgDarkGreen
        if (isRound) {
            g2.fill(Ellipse2D.Float(0f, 0f, size.toFloat(), size.toFloat()))
        } else {
            // Full bleed, edge-to-edge, NO white border, NO transparent rim
            g2.fillRect(0, 0, size, size)
        }

        // 2. Draw 3 Horizontal Rounded Bars of decreasing length
        // Proportional to 108dp canvas:
        // Top: w=48, h=8, r=4 (X: 30..78, Y: 36..44)
        // Mid: w=36, h=8, r=4 (X: 36..72, Y: 50..58)
        // Bot: w=24, h=8, r=4 (X: 42..66, Y: 64..72)
        // Total glyph height = 36dp (Y: 36..72) -> vertically centered at Y=54
        val scale = size.toFloat() / 108f
        g2.color = fgBrightGreen

        val barHeight = 8f * scale
        val barRadius = 4f * scale
        val barGap = 6f * scale
        val totalGlyphHeight = 36f * scale
        val startY = (size.toFloat() - totalGlyphHeight) / 2f

        val barWidths = floatArrayOf(48f * scale, 36f * scale, 24f * scale)

        for (i in 0 until 3) {
            val w = barWidths[i]
            val x = (size.toFloat() - w) / 2f
            val y = startY + i * (barHeight + barGap)
            val pill = RoundRectangle2D.Float(x, y, w, barHeight, barRadius * 2, barRadius * 2)
            g2.fill(pill)
        }

        g2.dispose()
        return img
    }

    @Test
    fun testGenerateAndVerifyAllMipmapIcons() {
        val workingDir = File(".").canonicalFile
        val resDir = if (File(workingDir, "app/src/main/res").exists()) {
            File(workingDir, "app/src/main/res")
        } else if (File(workingDir, "src/main/res").exists()) {
            File(workingDir, "src/main/res")
        } else {
            File(workingDir, "../app/src/main/res")
        }

        densities.forEach { (density, size) ->
            val dir = File(resDir, "mipmap-$density")
            dir.mkdirs()

            // 1. Square full bleed legacy icon
            val squareFile = File(dir, "ic_launcher.png")
            val squareImg = renderIcon(size, isRound = false)
            ImageIO.write(squareImg, "png", squareFile)

            assertTrue("Square icon must exist: ${squareFile.path}", squareFile.exists())
            assertTrue("Square icon must have content", squareFile.length() > 0)

            val readSquare = ImageIO.read(squareFile)
            assertEquals(size, readSquare.width)
            assertEquals(size, readSquare.height)
            // Verify full bleed corner (0,0) is opaque background color, not white
            val cornerRgb = readSquare.getRGB(0, 0)
            val cornerAlpha = (cornerRgb shr 24) and 0xFF
            val cornerRed = (cornerRgb shr 16) and 0xFF
            val cornerGreen = (cornerRgb shr 8) and 0xFF
            val cornerBlue = cornerRgb and 0xFF
            assertEquals("Corner alpha must be 255 (opaque full bleed)", 255, cornerAlpha)
            assertFalse("Corner must NOT be white border", cornerRed > 240 && cornerGreen > 240 && cornerBlue > 240)

            // 2. Round legacy icon
            val roundFile = File(dir, "ic_launcher_round.png")
            val roundImg = renderIcon(size, isRound = true)
            ImageIO.write(roundImg, "png", roundFile)

            assertTrue("Round icon must exist: ${roundFile.path}", roundFile.exists())
            assertTrue("Round icon must have content", roundFile.length() > 0)

            val readRound = ImageIO.read(roundFile)
            assertEquals(size, readRound.width)
            assertEquals(size, readRound.height)
            // Outer corner of round icon must be transparent
            val roundCornerAlpha = (readRound.getRGB(0, 0) shr 24) and 0xFF
            assertEquals("Outer corner of circular icon must be transparent", 0, roundCornerAlpha)
        }

        // 3. Play Store 512x512 icon
        val playStoreImg = renderIcon(512, isRound = false)
        val projectRoot = if (File(workingDir, "app").exists()) workingDir else workingDir.parentFile
        val playStoreFile = File(projectRoot, "play_store_512.png")
        ImageIO.write(playStoreImg, "png", playStoreFile)

        assertTrue("Play Store icon must exist", playStoreFile.exists())
        val readPlayStore = ImageIO.read(playStoreFile)
        assertEquals(512, readPlayStore.width)
        assertEquals(512, readPlayStore.height)
        // Full bleed, no transparency
        for (x in 0 until 512 step 64) {
            for (y in 0 until 512 step 64) {
                val alpha = (readPlayStore.getRGB(x, y) shr 24) and 0xFF
                assertEquals("Play Store icon must have NO transparency at ($x,$y)", 255, alpha)
            }
        }
    }
}
