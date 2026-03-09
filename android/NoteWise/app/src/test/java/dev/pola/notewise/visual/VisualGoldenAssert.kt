package dev.pola.notewise.visual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.fail

/**
 * Visual golden image assertion helper.
 *
 * Comparison algorithm mirrors alphaTab's VisualTestHelper / PixelMatch:
 * - YIQ perceptual color distance (threshold=0.3, same as alphaTab)
 * - Anti-aliased pixels are skipped (includeAA=false)
 * - Pass condition: differentPixels / (totalPixels - transparentPixels) ≤ tolerancePercent
 * - Size mismatch → immediate fail (save candidate, print error)
 * - On fail → save *.new.png + *.diff.png
 * - On pass → delete *.new.png + *.diff.png
 *
 * Phase 1 target: tolerancePercent = 1.0 (match alphaTab's standard).
 */
object VisualGoldenAssert {

    private const val UPDATE_ENV = "UPDATE_VISUAL_GOLDENS"

    // YIQ perceptual threshold (same as alphaTab PixelMatch threshold=0.3):
    // maxDelta^2 = 35215 * threshold^2
    private const val PIXEL_MATCH_THRESHOLD = 0.3
    private val MAX_DELTA_SQ = 35215.0 * PIXEL_MATCH_THRESHOLD * PIXEL_MATCH_THRESHOLD

    fun assertMatchesGolden(
        goldenName: String,
        actual: Bitmap,
        tolerancePercent: Double = 1.0
    ) {
        val moduleRoot = resolveModuleRoot()
        val goldenFile = File(moduleRoot, "app/src/test/resources/visual-goldens/$goldenName.png")
        val reportDir = File(moduleRoot, "app/build/reports/visual-tests")
        val updateGoldens = (System.getenv(UPDATE_ENV) ?: "false").equals("true", ignoreCase = true)

        reportDir.mkdirs()

        if (!goldenFile.exists()) {
            if (updateGoldens) {
                goldenFile.parentFile?.mkdirs()
                saveBitmap(goldenFile, actual)
                return
            }

            val newFile = File(reportDir, "$goldenName.new.png")
            newFile.parentFile?.mkdirs()
            saveBitmap(newFile, actual)
            fail(
                "Missing golden image: ${goldenFile.path}. " +
                    "Wrote candidate to ${newFile.path}. " +
                    "Re-run with $UPDATE_ENV=true to accept it."
            )
        }

        val expected = requireNotNull(BitmapFactory.decodeFile(goldenFile.absolutePath)) {
            "Failed to decode golden image: ${goldenFile.path}"
        }

        if (updateGoldens) {
            saveBitmap(goldenFile, actual)
            return
        }

        if (expected.width != actual.width || expected.height != actual.height) {
            val newFile = File(reportDir, "$goldenName.new.png")
            newFile.parentFile?.mkdirs()
            saveBitmap(newFile, actual)
            fail(
                "Golden size mismatch for $goldenName: " +
                    "expected ${expected.width}x${expected.height}, " +
                    "actual ${actual.width}x${actual.height}. " +
                    "Candidate: ${newFile.path}"
            )
        }

        val diffBitmap = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
        var differentPixels = 0
        var transparentPixels = 0

        for (y in 0 until actual.height) {
            for (x in 0 until actual.width) {
                val a = actual.getPixel(x, y)
                val e = expected.getPixel(x, y)

                // Exclude background pixels from the diff denominator.
                // alphaTab renders empty areas as RGBA(0,0,0,0); NoteWise renders Color.WHITE.
                // Both are treated as "background" so they don't inflate the diff ratio.
                if (isBackground(a) && isBackground(e)) {
                    transparentPixels++
                    diffBitmap.setPixel(x, y, Color.TRANSPARENT)
                    continue
                }

                val delta = colorDeltaSq(a, e)
                if (delta > MAX_DELTA_SQ) {
                    differentPixels++
                    diffBitmap.setPixel(x, y, Color.RED)
                } else {
                    // Blend slightly to show unchanged pixels in diff output (alphaTab diffMask=true style).
                    diffBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        val totalPixels = actual.width * actual.height
        val opaquePixels = (totalPixels - transparentPixels).coerceAtLeast(1)
        val differencePercent = differentPixels.toDouble() / opaquePixels.toDouble() * 100.0

        if (differencePercent > tolerancePercent) {
            val newFile = File(reportDir, "$goldenName.new.png")
            val diffFile = File(reportDir, "$goldenName.diff.png")
            newFile.parentFile?.mkdirs()
            saveBitmap(newFile, actual)
            saveBitmap(diffFile, diffBitmap)
            fail(
                "Visual mismatch for $goldenName: " +
                    "$differentPixels/$opaquePixels opaque pixels " +
                    "(${String.format("%.2f", differencePercent)}%) " +
                    "exceeds tolerance ${String.format("%.2f", tolerancePercent)}%. " +
                    "Files: ${newFile.path}, ${diffFile.path}"
            )
        }

        // Keep reports clean when test passes.
        File(reportDir, "$goldenName.new.png").delete()
        File(reportDir, "$goldenName.diff.png").delete()
    }

    // ── YIQ perceptual color distance (port of alphaTab PixelMatch) ──────────────

    /**
     * Squared YIQ color distance between two ARGB pixels.
     * Matches alphaTab's PixelMatch.colorDelta(). Max value = 35215.0.
     *
     * Pixels are composited onto white before comparison so that alphaTab's
     * transparent background (alpha=0) and NoteWise's white background
     * (ARGB 255,255,255,255) produce identical color values.
     */
    private fun colorDeltaSq(a: Int, b: Int): Double {
        val ac = compositeOnWhite(a)
        val bc = compositeOnWhite(b)

        val ra = Color.red(ac).toDouble();   val ga = Color.green(ac).toDouble();  val ba = Color.blue(ac).toDouble()
        val rb = Color.red(bc).toDouble();   val gb = Color.green(bc).toDouble();  val bb = Color.blue(bc).toDouble()

        val dy = (ra * 0.29889531 + ga * 0.58662247 + ba * 0.11448223) -
                 (rb * 0.29889531 + gb * 0.58662247 + bb * 0.11448223)
        val di = (ra * 0.59597799 - ga * 0.27417610 - ba * 0.32180189) -
                 (rb * 0.59597799 - gb * 0.27417610 - bb * 0.32180189)
        val dq = (ra * 0.21147017 - ga * 0.52261711 + ba * 0.31114694) -
                 (rb * 0.21147017 - gb * 0.52261711 + bb * 0.31114694)

        return 0.5053 * dy * dy + 0.299 * di * di + 0.1957 * dq * dq
    }

    /**
     * Returns true if the pixel represents background (should be excluded from diff denominator).
     * Treats both fully-transparent pixels (alphaTab renders RGBA(0,0,0,0) for empty areas)
     * and fully-white pixels (NoteWise renders Color.WHITE for background) as background.
     */
    private fun isBackground(pixel: Int): Boolean =
        Color.alpha(pixel) < 10 ||
        (Color.alpha(pixel) >= 250 &&
         Color.red(pixel) >= 250 &&
         Color.green(pixel) >= 250 &&
         Color.blue(pixel) >= 250)

    /**
     * Composites a pixel onto a white background using standard alpha-blending.
     * Fully-opaque pixels are returned unchanged; fully-transparent become white.
     * This normalises alphaTab's transparent canvas background to match NoteWise's
     * white background before the YIQ color comparison.
     */
    private fun compositeOnWhite(pixel: Int): Int {
        val alpha = Color.alpha(pixel)
        if (alpha >= 250) return pixel
        if (alpha < 10) return Color.WHITE
        val a = alpha / 255.0
        val r = (Color.red(pixel) * a + 255.0 * (1.0 - a)).toInt().coerceIn(0, 255)
        val g = (Color.green(pixel) * a + 255.0 * (1.0 - a)).toInt().coerceIn(0, 255)
        val b = (Color.blue(pixel) * a + 255.0 * (1.0 - a)).toInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }

    // ── I/O helpers ──────────────────────────────────────────────────────────────

    private fun saveBitmap(file: File, bitmap: Bitmap) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun resolveModuleRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile

        // Running from app module directly (common in Gradle test execution).
        if (File(cwd, "src/test").exists() && File(cwd, "build.gradle.kts").exists()) {
            return cwd.parentFile ?: cwd
        }

        // Running from NoteWise root.
        val appDir = File(cwd, "app")
        if (File(appDir, "src/test").exists() && File(appDir, "build.gradle.kts").exists()) {
            return cwd
        }

        return cwd
    }
}
