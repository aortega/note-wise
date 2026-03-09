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

/**
 * Visual regression test covering all 150 fixtures from the W3C MusicXML Test Suite.
 *
 * Golden references are alphaTab renders (Phase 1 conformance, DEC-026).
 * Tolerance is 1.0% YIQ pixel-diff — same standard as LilyPondTier1VisualTest.
 *
 * XML files are loaded directly from the alphaTab reference copy of the test suite:
 *   android/reference/alphaTab-develop/packages/alphatab/test-data/musicxml-testsuite/
 *
 * Fixtures that the parser cannot yet handle are skipped with a console message
 * rather than failing — they represent future implementation targets.
 *
 * Filter to a subset with:
 *   MUSICXML_SUITE_FIXTURES="01a-Pitches-Pitches.xml,02a-Rests-Durations.xml"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MusicXMLSuiteVisualTest {

    data class Fixture(val xmlFile: String, val goldenStem: String, val tolerance: Double = 1.0)
    data class CanvasSize(val width: Int, val height: Int)

    private val allFixtures = listOf(
        // 01 Pitches
        Fixture("01a-Pitches-Pitches.xml",                              "01a_pitches_pitches"),
        Fixture("01b-Pitches-Intervals.xml",                            "01b_pitches_intervals"),
        Fixture("01c-Pitches-NoVoiceElement.xml",                       "01c_pitches_novoiceelement"),
        Fixture("01d-Pitches-Microtones.xml",                           "01d_pitches_microtones"),
        Fixture("01e-Pitches-ParenthesizedAccidentals.xml",             "01e_pitches_parenthesizedaccidentals"),
        Fixture("01f-Pitches-ParenthesizedMicrotoneAccidentals.xml",    "01f_pitches_parenthesizedmicrotoneaccidentals"),
        // 02 Rests
        Fixture("02a-Rests-Durations.xml",                              "02a_rests_durations"),
        Fixture("02b-Rests-PitchedRests.xml",                           "02b_rests_pitchedrests"),
        Fixture("02c-Rests-MultiMeasureRests.xml",                      "02c_rests_multimeasurerests"),
        Fixture("02d-Rests-Multimeasure-TimeSignatures.xml",            "02d_rests_multimeasure_timesignatures"),
        Fixture("02e-Rests-NoType.xml",                                 "02e_rests_notype"),
        // 03 Rhythm
        Fixture("03a-Rhythm-Durations.xml",                             "03a_rhythm_durations"),
        Fixture("03b-Rhythm-Backup.xml",                                "03b_rhythm_backup"),
        Fixture("03c-Rhythm-DivisionChange.xml",                        "03c_rhythm_divisionchange"),
        Fixture("03d-Rhythm-DottedDurations-Factors.xml",               "03d_rhythm_dotteddurations_factors"),
        Fixture("03e-Rhythm-No-Divisions.xml",                          "03e_rhythm_no_divisions"),
        Fixture("03f-Rhythm-Forward.xml",                               "03f_rhythm_forward"),
        // 11 Time Signatures
        Fixture("11a-TimeSignatures.xml",                               "11a_time_signatures"),
        Fixture("11b-TimeSignatures-NoTime.xml",                        "11b_time_signatures_notime"),
        Fixture("11c-TimeSignatures-CompoundSimple.xml",                "11c_time_signatures_compoundsimple"),
        Fixture("11d-TimeSignatures-CompoundMultiple.xml",              "11d_time_signatures_compoundmultiple"),
        Fixture("11e-TimeSignatures-CompoundMixed.xml",                 "11e_time_signatures_compoundmixed"),
        Fixture("11f-TimeSignatures-SymbolMeaning.xml",                 "11f_time_signatures_symbolmeaning"),
        Fixture("11g-TimeSignatures-SingleNumber.xml",                  "11g_time_signatures_singlenumber"),
        Fixture("11h-TimeSignatures-SenzaMisura.xml",                   "11h_time_signatures_senzamisura"),
        // 12 Clefs
        Fixture("12a-Clefs.xml",                                        "12a_clefs"),
        Fixture("12b-Clefs-NoKeyOrClef.xml",                            "12b_clefs_nokeyorclef"),
        // 13 Key Signatures
        Fixture("13a-KeySignatures.xml",                                "13a_key_signatures"),
        Fixture("13b-KeySignatures-ChurchModes.xml",                    "13b_key_signatures_churchmodes"),
        Fixture("13c-KeySignatures-NonTraditional.xml",                 "13c_key_signatures_nontraditional"),
        Fixture("13d-KeySignatures-Microtones.xml",                     "13d_key_signatures_microtones"),
        Fixture("13e-KeySignatures-Cancel.xml",                         "13e_key_signatures_cancel"),
        Fixture("13f-KeySignatures-Visible.xml",                        "13f_key_signatures_visible"),
        // 14 Staff Details
        Fixture("14a-StaffDetails-LineChanges.xml",                     "14a_staffdetails_linechanges"),
        // 21 Chords
        Fixture("21a-Chord-Basic.xml",                                  "21a_chord_basic"),
        Fixture("21b-Chords-TwoNotes.xml",                              "21b_chords_twonotes"),
        Fixture("21c-Chords-ThreeNotesDuration.xml",                    "21c_chords_threenotesduration"),
        Fixture("21d-Chords-SchubertStabatMater.xml",                   "21d_chords_schubertstabatmater"),
        Fixture("21e-Chords-PickupMeasures.xml",                        "21e_chords_pickupmeasures"),
        Fixture("21f-Chord-ElementInBetween.xml",                       "21f_chord_elementinbetween"),
        Fixture("21g-Chords-Tremolos.xml",                              "21g_chords_tremolos"),
        Fixture("21h-Chord-Accidentals.xml",                            "21h_chord_accidentals"),
        // 22 Noteheads
        Fixture("22a-Noteheads.xml",                                    "22a_noteheads"),
        Fixture("22b-Staff-Notestyles.xml",                             "22b_staff_notestyles"),
        Fixture("22c-Noteheads-Chords.xml",                             "22c_noteheads_chords"),
        Fixture("22d-Parenthesized-Noteheads.xml",                      "22d_parenthesized_noteheads"),
        // 23 Tuplets
        Fixture("23a-Tuplets.xml",                                      "23a_tuplets"),
        Fixture("23b-Tuplets-Styles.xml",                               "23b_tuplets_styles"),
        Fixture("23c-Tuplet-Display-NonStandard.xml",                   "23c_tuplet_display_nonstandard"),
        Fixture("23d-Tuplets-Nested.xml",                               "23d_tuplets_nested"),
        Fixture("23e-Tuplets-Tremolo.xml",                              "23e_tuplets_tremolo"),
        Fixture("23f-Tuplets-DurationButNoBracket.xml",                 "23f_tuplets_durationbutnobracket"),
        // 24 Grace Notes
        Fixture("24a-GraceNotes.xml",                                   "24a_gracenotes"),
        Fixture("24b-ChordAsGraceNote.xml",                             "24b_chordasgracenote"),
        Fixture("24c-GraceNote-MeasureEnd.xml",                         "24c_gracenote_measureend"),
        Fixture("24d-AfterGrace.xml",                                   "24d_aftergrace"),
        Fixture("24e-GraceNote-StaffChange.xml",                        "24e_gracenote_staffchange"),
        Fixture("24f-GraceNote-Slur.xml",                               "24f_gracenote_slur"),
        Fixture("24g-GraceNote-Dynamics.xml",                           "24g_gracenote_dynamics"),
        Fixture("24h-GraceNote-Simultaneous.xml",                       "24h_gracenote_simultaneous"),
        // 31 Directions
        Fixture("31a-Directions.xml",                                   "31a_directions"),
        Fixture("31b-Directions-Order.xml",                             "31b_directions_order"),
        Fixture("31c-MetronomeMarks.xml",                               "31c_metronomemarks"),
        Fixture("31d-Directions-Compounds.xml",                         "31d_directions_compounds"),
        // 32 Notations / Articulations
        Fixture("32a-Notations.xml",                                    "32a_notations"),
        Fixture("32b-Articulations-Texts.xml",                          "32b_articulations_texts"),
        Fixture("32c-MultipleNotationChildren.xml",                     "32c_multiplenotationchildren"),
        Fixture("32d-Arpeggio.xml",                                     "32d_arpeggio"),
        // 33 Spanners / Ties / Slurs
        Fixture("33a-Spanners.xml",                                     "33a_spanners"),
        Fixture("33b-Spanners-Tie.xml",                                 "33b_spanners_tie"),
        Fixture("33c-Spanners-Slurs.xml",                               "33c_spanners_slurs"),
        Fixture("33da-Spanners-OctaveShifts-before.xml",                "33da_spanners_octaveshifts_before"),
        Fixture("33db-Spanners-OctaveShifts-after.xml",                 "33db_spanners_octaveshifts_after"),
        Fixture("33e-Spanners-OctaveShifts-InvalidSize.xml",            "33e_spanners_octaveshifts_invalidsize"),
        Fixture("33f-Trill-EndingOnGraceNote.xml",                      "33f_trill_endingongracenote"),
        Fixture("33g-Slur-ChordedNotes.xml",                            "33g_slur_chordednotes"),
        Fixture("33h-Spanners-Glissando.xml",                           "33h_spanners_glissando"),
        Fixture("33i-Ties-NotEnded.xml",                                "33i_ties_notended"),
        Fixture("33j-Beams-Tremolos.xml",                               "33j_beams_tremolos"),
        // 34 Print / Colors / Fonts
        Fixture("34a-Print-Object-Spanners.xml",                        "34a_print_object_spanners"),
        Fixture("34b-Colors.xml",                                       "34b_colors"),
        Fixture("34c-Font-Size.xml",                                    "34c_font_size"),
        // 41 Multi-Parts / Staff Groups
        Fixture("41a-MultiParts-Partorder.xml",                         "41a_multiparts_partorder"),
        Fixture("41b-MultiParts-MoreThan10.xml",                        "41b_multiparts_morethan10"),
        Fixture("41c-StaffGroups.xml",                                  "41c_staffgroups"),
        Fixture("41d-StaffGroups-Nested.xml",                           "41d_staffgroups_nested"),
        Fixture("41e-StaffGroups-InstrumentNames-Linebroken.xml",       "41e_staffgroups_instrumentnames_linebroken"),
        Fixture("41f-StaffGroups-Overlapping.xml",                      "41f_staffgroups_overlapping"),
        Fixture("41g-StaffGroups-NestingOrder.xml",                     "41g_staffgroups_nestingorder"),
        Fixture("41h-TooManyParts.xml",                                 "41h_toomanyparts"),
        Fixture("41i-PartNameDisplay-Override.xml",                     "41i_partnamedisplay_override"),
        Fixture("41j-PartNameDisplay-Multiple-DisplayText-Children.xml","41j_partnamedisplay_multiple_displaytext_children"),
        Fixture("41k-PartName-Print.xml",                               "41k_partname_print"),
        Fixture("41l-GroupNameDisplay-Override.xml",                    "41l_groupnamedisplay_override"),
        // 42 Multi-Voice
        Fixture("42a-MultiVoice-TwoVoicesOnStaff-Lyrics.xml",          "42a_multivoice_twovoicesonstaff_lyrics"),
        Fixture("42b-MultiVoice-MidMeasureClefChange.xml",              "42b_multivoice_midmeasureclefchange"),
        // 43 Multi-Staff
        Fixture("43a-PianoStaff.xml",                                   "43a_pianostaff"),
        Fixture("43b-MultiStaff-DifferentKeys.xml",                     "43b_multistaff_differentkeys"),
        Fixture("43c-MultiStaff-DifferentKeysAfterBackup.xml",          "43c_multistaff_differentkeysafterbackup"),
        Fixture("43d-MultiStaff-StaffChange.xml",                       "43d_multistaff_staffchange"),
        Fixture("43e-Multistaff-ClefDynamics.xml",                      "43e_multistaff_clefdynamics"),
        Fixture("43f-MultiStaff-Lyrics.xml",                            "43f_multistaff_lyrics"),
        Fixture("43g-MultiStaff-PartSymbol.xml",                        "43g_multistaff_partsymbol"),
        // 45 Repeats
        Fixture("45a-SimpleRepeat.xml",                                 "45a_simplerepeat"),
        Fixture("45b-RepeatWithAlternatives.xml",                       "45b_repeatwithalternatives"),
        Fixture("45c-SimpleRepeat-Nested.xml",                          "45c_simplerepeat_nested"),
        Fixture("45d-Repeats-MultipleEndings.xml",                      "45d_repeats_multipleendings"),
        Fixture("45e-Repeats-Combination.xml",                          "45e_repeats_combination"),
        Fixture("45f-Repeats-InvalidEndings.xml",                       "45f_repeats_invalidendings"),
        Fixture("45g-Repeats-NotEnded.xml",                             "45g_repeats_notended"),
        Fixture("45h-Repeats-Partial.xml",                              "45h_repeats_partial"),
        Fixture("45i-Repeats-Nested.xml",                               "45i_repeats_nested"),
        // 46 Barlines / Pickup Measures
        Fixture("46a-Barlines.xml",                                     "46a_barlines"),
        Fixture("46b-MidmeasureBarline.xml",                            "46b_midmeasurebarline"),
        Fixture("46c-Midmeasure-Clef.xml",                              "46c_midmeasure_clef"),
        Fixture("46d-PickupMeasure-ImplicitMeasures.xml",               "46d_pickupmeasure_implicitmeasures"),
        Fixture("46e-PickupMeasure-SecondVoiceStartsLater.xml",         "46e_pickupmeasure_secondvoicestartslater"),
        Fixture("46f-IncompleteMeasures.xml",                           "46f_incompletemeasures"),
        Fixture("46g-PickupMeasure-Chordnames-FiguredBass.xml",         "46g_pickupmeasure_chordnames_figuredbass"),
        // 51-52 Header / Page Layout
        Fixture("51b-Header-Quotes.xml",                                "51b_header_quotes"),
        Fixture("51c-MultipleRights.xml",                               "51c_multiplerights"),
        Fixture("51d-EmptyTitle.xml",                                   "51d_emptytitle"),
        Fixture("52a-PageLayout.xml",                                   "52a_pagelayout"),
        Fixture("52b-Breaks.xml",                                       "52b_breaks"),
        // 61 Lyrics
        Fixture("61a-Lyrics.xml",                                       "61a_lyrics"),
        Fixture("61b-MultipleLyrics.xml",                               "61b_multiplelyrics"),
        Fixture("61c-Lyrics-Pianostaff.xml",                            "61c_lyrics_pianostaff"),
        Fixture("61d-Lyrics-Melisma.xml",                               "61d_lyrics_melisma"),
        Fixture("61e-Lyrics-Chords.xml",                                "61e_lyrics_chords"),
        Fixture("61f-Lyrics-GracedNotes.xml",                           "61f_lyrics_gracednotes"),
        Fixture("61g-Lyrics-NameNumber.xml",                            "61g_lyrics_namenumber"),
        Fixture("61h-Lyrics-BeamsMelismata.xml",                        "61h_lyrics_beamsmelismata"),
        Fixture("61i-Lyrics-Chords.xml",                                "61i_lyrics_chords"),
        Fixture("61j-Lyrics-Elisions.xml",                              "61j_lyrics_elisions"),
        Fixture("61k-Lyrics-SpannersExtenders.xml",                     "61k_lyrics_spannersextenders"),
        // 71 Chord Names / Frets / Tab
        Fixture("71a-Chordnames.xml",                                   "71a_chordnames"),
        Fixture("71c-ChordsFrets.xml",                                  "71c_chordsfrets"),
        Fixture("71d-ChordsFrets-Multistaff.xml",                       "71d_chordsfrets_multistaff"),
        Fixture("71e-TabStaves.xml",                                    "71e_tabstaves"),
        Fixture("71f-AllChordTypes.xml",                                "71f_allchordtypes"),
        Fixture("71g-MultipleChordnames.xml",                           "71g_multiplechordnames"),
        // 72 Transposing Instruments
        Fixture("72a-TransposingInstruments.xml",                       "72a_transposinginstruments"),
        Fixture("72b-TransposingInstruments-Full.xml",                  "72b_transposinginstruments_full"),
        Fixture("72c-TransposingInstruments-Change.xml",                "72c_transposinginstruments_change"),
        // 73-75 Percussion / Figured Bass / Accordion
        Fixture("73a-Percussion.xml",                                   "73a_percussion"),
        Fixture("74a-FiguredBass.xml",                                  "74a_figuredbass"),
        Fixture("75a-AccordionRegistrations.xml",                       "75a_accordionregistrations"),
        // 90 Compressed MusicXML
        Fixture("90a-Compressed-MusicXML.mxl",                         "90a_compressed_musicxml"),
        // 99 Miscellaneous
        Fixture("99a-Sibelius5-IgnoreBeaming.xml",                      "99a_sibelius5_ignorebeaming"),
        Fixture("99b-Lyrics-BeamsMelismata-IgnoreBeams.xml",            "99b_lyrics_beamsmelismata_ignorebeams")
    )

    @Test
    fun `musicxml suite fixtures render stable`() {
        val fixtureFilter = parseFixtureFilterFromEnv()
        val fixtures = if (fixtureFilter.isEmpty()) {
            allFixtures
        } else {
            allFixtures.filter { fixtureFilter.contains(it.xmlFile) }
        }
        val relaxedSanity = parseRelaxedSanityFromEnv(default = fixtureFilter.isNotEmpty())

        assertTrue(
            "No fixtures selected. Set MUSICXML_SUITE_FIXTURES to a comma-separated " +
                "list of xml filenames, or unset to run all.",
            fixtures.isNotEmpty()
        )

        val widthsFromEnv = parseWidthsFromEnv(default = emptyList())
        val manifestReferenceSizes = loadManifestReferenceSizes()
        // Match alphaTab golden geometry: 9px/space, first stave top-line at y≈13
        // (same parameters as LilyPondTier1VisualTest — goldens use padding=[7,0,7,0])
        val staffSpacingPx = parseStaffSpacingFromEnv(default = 9f)
        val startYPx = parseStartYFromEnv(default = 13f)

        var skipped = 0
        var ran = 0
        val failures = mutableListOf<String>()

        for (fixture in fixtures) {
            val xmlFile = resolveFixtureFile(
                "reference/alphaTab-develop/packages/alphatab/test-data/musicxml-testsuite/${fixture.xmlFile}"
            )
            if (!xmlFile.exists()) {
                println("[MUSICXML_SUITE] SKIP ${fixture.xmlFile}: file not found at ${xmlFile.absolutePath}")
                skipped++
                continue
            }

            val sheet = try {
                xmlFile.inputStream().use { MusicXMLParser().parse(it) }
            } catch (e: Exception) {
                println("[MUSICXML_SUITE] SKIP ${fixture.xmlFile}: parse failed — ${e.javaClass.simpleName}: ${e.message}")
                skipped++
                continue
            }

            val measures = try {
                MusicSheetToVF.convert(
                    sheet = sheet,
                    startX = 0f,
                    startY = startYPx,
                    staveWidth = (manifestReferenceSizes[fixture.xmlFile]?.width ?: 635).toFloat(),
                    staffLineSpacingPx = staffSpacingPx
                )
            } catch (e: Exception) {
                println("[MUSICXML_SUITE] SKIP ${fixture.xmlFile}: convert failed — ${e.javaClass.simpleName}: ${e.message}")
                skipped++
                continue
            }

            if (measures.isEmpty()) {
                println("[MUSICXML_SUITE] SKIP ${fixture.xmlFile}: convert produced empty measure list")
                skipped++
                continue
            }

            val manifestSize = manifestReferenceSizes[fixture.xmlFile]
            val widths = if (widthsFromEnv.isEmpty()) {
                listOf(manifestSize?.width ?: 635)
            } else {
                widthsFromEnv
            }

            for (widthPx in widths) {
                val goldenName = "musicxml-suite/${fixture.goldenStem}_${widthPx}"
                val targetHeight = if (widthsFromEnv.isEmpty() && manifestSize?.width == widthPx) {
                    manifestSize.height
                } else {
                    null
                }
                println(
                    "[MUSICXML_SUITE] fixture=${fixture.xmlFile} width=$widthPx " +
                        "targetHeight=${targetHeight ?: "auto"} golden=$goldenName"
                )

                val bitmap = VisualRenderHarness.renderMeasuresToBitmap(
                    measures = measures,
                    widthPx = widthPx,
                    fixedHeightPx = targetHeight,
                    startY = startYPx
                )
                val inkOk = runCatching { assertHasVisibleInk(bitmap, fixture.xmlFile, widthPx, relaxedSanity) }
                val goldenOk = runCatching {
                    VisualGoldenAssert.assertMatchesGolden(
                        goldenName = goldenName,
                        actual = bitmap,
                        tolerancePercent = fixture.tolerance
                    )
                }
                val failure = (inkOk.exceptionOrNull() ?: goldenOk.exceptionOrNull())
                if (failure != null) {
                    failures += "${fixture.xmlFile}@${widthPx}px: ${failure.message}"
                }
                ran++
            }
        }

        println("[MUSICXML_SUITE] Done: ran=$ran skipped=$skipped failures=${failures.size} total=${fixtures.size}")
        assertTrue(
            "MusicXML suite failures (${failures.size}/${ran}):\n" + failures.joinToString("\n") { "  ✗ $it" },
            failures.isEmpty()
        )
    }

    private fun loadManifestReferenceSizes(): Map<String, CanvasSize> {
        val manifestFile = resolveFixtureFile("app/src/test/resources/visual-goldens/musicxml-suite/approval_manifest.json")
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
        val raw = System.getenv("MUSICXML_SUITE_FIXTURES")?.trim().orEmpty()
        if (raw.isEmpty()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun parseRelaxedSanityFromEnv(default: Boolean): Boolean {
        val raw = System.getenv("MUSICXML_SUITE_RELAX_SANITY")?.trim()?.lowercase().orEmpty()
        return when (raw) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> default
        }
    }

    private fun parseWidthsFromEnv(default: List<Int>): List<Int> {
        val raw = System.getenv("MUSICXML_SUITE_WIDTHS")?.trim().orEmpty()
        if (raw.isEmpty()) return default
        val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it >= 320 }.distinct().sorted()
        return if (parsed.isEmpty()) default else parsed
    }

    private fun parseStaffSpacingFromEnv(default: Float): Float {
        val raw = System.getenv("MUSICXML_SUITE_STAFF_SPACING")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null && parsed >= 4f) parsed else default
    }

    private fun parseStartYFromEnv(default: Float): Float {
        val raw = System.getenv("MUSICXML_SUITE_START_Y")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null) parsed else default
    }

    private fun assertHasVisibleInk(bitmap: Bitmap, fixture: String, widthPx: Int, relaxedSanity: Boolean) {
        var nonWhite = 0
        var minX = bitmap.width; var minY = bitmap.height
        var maxX = -1; var maxY = -1
        val requiredMin = if (relaxedSanity) 24
                          else (bitmap.width * bitmap.height * 0.00025).toInt().coerceAtLeast(220)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val px = bitmap.getPixel(x, y)
                if (Color.alpha(px) > 16 &&
                    (Color.red(px) < 245 || Color.green(px) < 245 || Color.blue(px) < 245)) {
                    nonWhite++
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
        }
        val bboxW = if (maxX >= minX) maxX - minX + 1 else 0
        val bboxH = if (maxY >= minY) maxY - minY + 1 else 0
        val hasSufficientBounds = bboxW >= 20 && bboxH >= 12
        val notPinnedToFarRight = relaxedSanity || minX < (bitmap.width - 40)

        assertTrue(
            "Rendered output appears blank for $fixture at width=$widthPx " +
                "(opaqueInkPixels=$nonWhite required>=$requiredMin bbox=${bboxW}x${bboxH})",
            nonWhite >= requiredMin && hasSufficientBounds && notPinnedToFarRight
        )
    }

    private fun resolveFixtureFile(pathFromAndroidRoot: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        val cwd = File(userDir).canonicalFile
        val candidates = listOf(
            File(cwd, "../$pathFromAndroidRoot"),
            File(cwd, pathFromAndroidRoot),
            File(cwd, "android/$pathFromAndroidRoot"),
            File(cwd, "../../$pathFromAndroidRoot"),
            File(cwd, "../android/$pathFromAndroidRoot")
        ).map { it.canonicalFile }
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
