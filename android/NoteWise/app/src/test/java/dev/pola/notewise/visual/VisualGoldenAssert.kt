package dev.pola.notewise.visual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.Assert.fail

object VisualGoldenAssert {

    private const val UPDATE_ENV = "UPDATE_VISUAL_GOLDENS"

    fun assertMatchesGolden(
        goldenName: String,
        actual: Bitmap,
        tolerancePercent: Double = 0.8,
        channelTolerance: Int = 18
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
                    "Wrote candidate image to ${newFile.path}. " +
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
            saveBitmap(newFile, actual)
            fail(
                "Golden size mismatch for $goldenName: expected ${expected.width}x${expected.height}, " +
                    "actual ${actual.width}x${actual.height}. Candidate: ${newFile.path}"
            )
        }

        val diffBitmap = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
        var mismatchPixels = 0
        val totalPixels = actual.width * actual.height

        for (y in 0 until actual.height) {
            for (x in 0 until actual.width) {
                val a = actual.getPixel(x, y)
                val e = expected.getPixel(x, y)

                val mismatch =
                    abs(Color.alpha(a) - Color.alpha(e)) > channelTolerance ||
                        abs(Color.red(a) - Color.red(e)) > channelTolerance ||
                        abs(Color.green(a) - Color.green(e)) > channelTolerance ||
                        abs(Color.blue(a) - Color.blue(e)) > channelTolerance

                if (mismatch) {
                    mismatchPixels++
                    diffBitmap.setPixel(x, y, Color.RED)
                } else {
                    diffBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        val differencePercent = (mismatchPixels.toDouble() / totalPixels.toDouble()) * 100.0
        if (differencePercent > tolerancePercent) {
            val newFile = File(reportDir, "$goldenName.new.png")
            val diffFile = File(reportDir, "$goldenName.diff.png")
            newFile.parentFile?.mkdirs()

            saveBitmap(newFile, actual)
            saveBitmap(diffFile, diffBitmap)

            fail(
                "Visual mismatch for $goldenName: " +
                    "$mismatchPixels/$totalPixels pixels (${String.format("%.2f", differencePercent)}%) " +
                    "exceeds tolerance ${String.format("%.2f", tolerancePercent)}%. " +
                    "Files: ${newFile.path}, ${diffFile.path}"
            )
        }

        // Keep reports clean when test passes.
        File(reportDir, "$goldenName.new.png").delete()
        File(reportDir, "$goldenName.diff.png").delete()
    }

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
