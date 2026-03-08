package dev.pola.notewise.visual

import android.graphics.Bitmap
import android.graphics.Color
import dev.pola.vexflow.parser.MusicSheetToVF
import dev.pola.vexflow.parser.MusicXMLParser
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LilyPondTier1VisualTest {

    data class Fixture(val xmlFile: String, val goldenStem: String, val tolerance: Double)
    data class CanvasSize(val width: Int, val height: Int)

    @Test
    fun `tier1 lilypond fixtures render stable`() {
        val allFixtures = listOf(
            Fixture("01a-Pitches-Pitches.xml", "01a_pitches_pitches", 2.8),
            Fixture("11a-TimeSignatures.xml", "11a_time_signatures", 2.6),
            Fixture("12aa-Clefs_Pitch_Traditional.xml", "12aa_clefs_pitch_traditional", 2.8),
            Fixture("13a-KeySignatures.xml", "13a_key_signatures", 2.8)
        )

        val fixtureFilter = parseFixtureFilterFromEnv()
        val fixtures = if (fixtureFilter.isEmpty()) {
            allFixtures
        } else {
            allFixtures.filter { fixtureFilter.contains(it.xmlFile) }
        }
        val relaxedSanity = parseRelaxedSanityFromEnv(default = fixtureFilter.isNotEmpty())

        assertTrue(
            "No fixtures selected for LilyPond tier1 test. " +
                "Set LILYPOND_FIXTURES to a comma-separated subset of xml fixture names " +
                "or unset it to run all fixtures.",
            fixtures.isNotEmpty()
        )

        val widthsFromEnv = parseWidthsFromEnv(default = emptyList())
        val manifestReferenceSizes = loadManifestReferenceSizes()
        val staffSpacingPx = parseStaffSpacingFromEnv(default = 7f)
        val startYPx = parseStartYFromEnv(default = 8f)

        for (fixture in fixtures) {
            val xmlFile = resolveFixtureFile("samples/lilypond_tests/xml_files/${fixture.xmlFile}")
            assertTrue("Fixture should exist: ${xmlFile.absolutePath}", xmlFile.exists())

            val sheet = xmlFile.inputStream().use { MusicXMLParser().parse(it) }
            val totalMeasures = sheet.parts.sumOf { it.measures.size }
            val totalEvents = sheet.parts.sumOf { part -> part.measures.sumOf { it.notes.size } }
            assertTrue("Parsed sheet should contain measures for ${fixture.xmlFile}", totalMeasures > 0)
            assertTrue("Parsed sheet should contain notes/rest events for ${fixture.xmlFile}", totalEvents > 0)

            val manifestSize = manifestReferenceSizes[fixture.xmlFile]
            val widths = if (widthsFromEnv.isEmpty()) {
                listOf(manifestSize?.width ?: 720)
            } else {
                widthsFromEnv
            }

            for (widthPx in widths) {
                val goldenName = "lilypond/tier1/${fixture.goldenStem}_${widthPx}"
                val targetHeight = if (widthsFromEnv.isEmpty() && manifestSize?.width == widthPx) {
                    manifestSize.height
                } else {
                    null
                }
                println(
                    "[LilyPondTier1VisualTest] fixture=${fixture.xmlFile} " +
                        "width=$widthPx targetHeight=${targetHeight ?: "auto"} " +
                        "spacing=$staffSpacingPx golden=$goldenName"
                )

                val measures = MusicSheetToVF.convert(
                    sheet = sheet,
                    startX = 0f,
                    startY = startYPx,
                    staveWidth = widthPx.toFloat(),
                    staffLineSpacingPx = staffSpacingPx
                )
                assertFalse("Converted measures should not be empty for ${fixture.xmlFile}", measures.isEmpty())

                val bitmap = VisualRenderHarness.renderMeasuresToBitmap(
                    measures = measures,
                    widthPx = widthPx,
                    fixedHeightPx = targetHeight,
                    startY = startYPx
                )
                assertHasVisibleInk(bitmap, fixture.xmlFile, widthPx, relaxedSanity)
                VisualGoldenAssert.assertMatchesGolden(
                    goldenName = goldenName,
                    actual = bitmap,
                    tolerancePercent = fixture.tolerance
                )
            }
        }
    }

    private fun parseWidthsFromEnv(default: List<Int>): List<Int> {
        val raw = System.getenv("LILYPOND_VISUAL_WIDTHS")?.trim().orEmpty()
        if (raw.isEmpty()) return default

        val parsed = raw.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 320 }
            .distinct()
            .sorted()

        return if (parsed.isEmpty()) default else parsed
    }

    private fun loadManifestReferenceSizes(): Map<String, CanvasSize> {
        val manifestFile = resolveFixtureFile("app/src/test/resources/visual-goldens/lilypond/tier1/approval_manifest.json")
        if (!manifestFile.exists()) return emptyMap()

        val raw = manifestFile.readText()
        val root = JSONObject(raw)
        val result = mutableMapOf<String, CanvasSize>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val fixtureName = keys.next()
            val entry = root.optJSONObject(fixtureName) ?: continue
            val size = entry.optJSONArray("reference_size") ?: continue
            if (size.length() < 2) continue
            val width = size.optInt(0, -1)
            val height = size.optInt(1, -1)
            if (width <= 0 || height <= 0) continue
            result[fixtureName] = CanvasSize(width = width, height = height)
        }
        return result
    }

    private fun parseFixtureFilterFromEnv(): Set<String> {
        val raw = System.getenv("LILYPOND_FIXTURES")?.trim().orEmpty()
        if (raw.isEmpty()) return emptySet()

        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun parseRelaxedSanityFromEnv(default: Boolean): Boolean {
        val raw = System.getenv("LILYPOND_RELAX_SANITY")?.trim()?.lowercase().orEmpty()
        return when (raw) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> default
        }
    }

    private fun parseStaffSpacingFromEnv(default: Float): Float {
        val raw = System.getenv("LILYPOND_STAFF_SPACING")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null && parsed >= 4f) parsed else default
    }

    private fun parseStartYFromEnv(default: Float): Float {
        val raw = System.getenv("LILYPOND_START_Y")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null) parsed else default
    }

    private fun assertHasVisibleInk(bitmap: Bitmap, fixture: String, widthPx: Int, relaxedSanity: Boolean) {
        var nonWhite = 0
        var nonWhiteAnyAlpha = 0
        var minX = bitmap.width
        var minY = bitmap.height
        var maxX = -1
        var maxY = -1
        // Sanity guard only: reject truly empty renders, but allow sparse fixtures.
        // In fixture-filtered progressive approval runs, default to relaxed mode so candidate
        // images can still be produced and reviewed even when rendering is visibly wrong.
        val requiredMin = if (relaxedSanity) {
            24
        } else {
            (bitmap.width * bitmap.height * 0.00025).toInt().coerceAtLeast(220)
        }

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val px = bitmap.getPixel(x, y)
                // White background is expected; any darker pixel indicates rendered notation ink.
                val isNonWhite = Color.red(px) < 245 || Color.green(px) < 245 || Color.blue(px) < 245
                if (isNonWhite) {
                    nonWhiteAnyAlpha++
                    if (Color.alpha(px) > 16) {
                        nonWhite++
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                        if (nonWhite >= requiredMin) {
                            // Keep scanning to compute robust bounds and avoid tiny edge clusters.
                        }
                    }
                }
            }
        }

        val bboxWidth = if (maxX >= minX) (maxX - minX + 1) else 0
        val bboxHeight = if (maxY >= minY) (maxY - minY + 1) else 0
        val hasSufficientBounds = if (relaxedSanity) {
            bboxWidth >= 4 && bboxHeight >= 8
        } else {
            bboxWidth >= 40 && bboxHeight >= 24
        }
        val notPinnedToFarRight = if (relaxedSanity) {
            true
        } else {
            minX < (bitmap.width - 40)
        }
        val passes = nonWhite >= requiredMin && hasSufficientBounds && notPinnedToFarRight

        assertTrue(
            "Rendered output appears blank for $fixture at width=$widthPx " +
                "(relaxedSanity=$relaxedSanity, opaqueInkPixels=$nonWhite, " +
                "anyAlphaInkPixels=$nonWhiteAnyAlpha, required>=$requiredMin, " +
                "bbox=($minX,$minY)-($maxX,$maxY), bboxSize=${bboxWidth}x${bboxHeight}, " +
                "notPinnedToFarRight=$notPinnedToFarRight)",
            passes
        )
    }


    private fun resolveFixtureFile(pathFromAndroidRoot: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        val cwd = File(userDir).canonicalFile
        val candidates = listOf(
            // Running from android/NoteWise
            File(cwd, "../$pathFromAndroidRoot"),
            // Running from android root
            File(cwd, pathFromAndroidRoot),
            // Running from repo root
            File(cwd, "android/$pathFromAndroidRoot"),
            // Fallbacks for uncommon cwd variants
            File(cwd, "../../$pathFromAndroidRoot"),
            File(cwd, "../android/$pathFromAndroidRoot")
        ).map { it.canonicalFile }

        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
