package dev.pola.notewise.visual

import android.graphics.Bitmap
import dev.pola.vexflow.parser.MusicSheetToVF
import dev.pola.vexflow.parser.MusicXMLParser
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GrandStaffVisualTest {

    @Test
    fun `clair de lune full renders stable grand staff at width 1080`() {
                assertClairGolden(widthPx = 1080, golden = "grandstaff/clair_full_1080", tolerance = 1.2)
        }

        @Test
        fun `clair de lune full renders stable grand staff at width 720`() {
                assertClairGolden(widthPx = 720, golden = "grandstaff/clair_full_720", tolerance = 1.4)
        }

        @Test
        fun `clair de lune full renders stable grand staff at width 1440`() {
                assertClairGolden(widthPx = 1440, golden = "grandstaff/clair_full_1440", tolerance = 1.2)
        }

        @Test
        fun `two-staff fixture keeps grouped staves and stable spacing`() {
                val sheet = parseFixture(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.1 Partwise//EN"
                        "http://www.musicxml.org/dtds/partwise.dtd">
                        <score-partwise version="3.1">
                            <part-list>
                                <score-part id="P1"><part-name>Piano</part-name></score-part>
                            </part-list>
                            <part id="P1">
                                <measure number="1">
                                    <attributes>
                                        <divisions>1</divisions>
                                        <staves>2</staves>
                                        <key><fifths>0</fifths></key>
                                        <time><beats>4</beats><beat-type>4</beat-type></time>
                                        <clef number="1"><sign>G</sign><line>2</line></clef>
                                        <clef number="2"><sign>F</sign><line>4</line></clef>
                                    </attributes>
                                    <note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>
                                    <note><pitch><step>D</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>
                                    <note><pitch><step>E</step><octave>3</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>2</staff></note>
                                    <note><pitch><step>F</step><octave>3</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>2</staff></note>
                                </measure>
                                <measure number="2">
                                    <note><pitch><step>G</step><octave>5</octave></pitch><duration>4</duration><voice>1</voice><type>whole</type><staff>1</staff></note>
                                    <note><pitch><step>C</step><octave>3</octave></pitch><duration>4</duration><voice>1</voice><type>whole</type><staff>2</staff></note>
                                </measure>
                            </part>
                        </score-partwise>
                        """.trimIndent()
                )

                val widthPx = 900
                val measures = MusicSheetToVF.convert(
                        sheet = sheet,
                        startX = 20f,
                        startY = 70f,
                        staveWidth = (widthPx - 40).toFloat()
                )

                val bitmap = VisualRenderHarness.renderMeasuresToBitmap(
                    measures = measures,
                    widthPx = widthPx
                )
                VisualGoldenAssert.assertMatchesGolden(
                        goldenName = "grandstaff/two_staff_fixture_900",
                        actual = bitmap,
                        tolerancePercent = 1.0
                )
        }

        @Test
        fun `mid-score key and time changes render stable signatures`() {
                val sheet = parseFixture(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.1 Partwise//EN"
                        "http://www.musicxml.org/dtds/partwise.dtd">
                        <score-partwise version="3.1">
                            <part-list>
                                <score-part id="P1"><part-name>Piano</part-name></score-part>
                            </part-list>
                            <part id="P1">
                                <measure number="1">
                                    <attributes>
                                        <divisions>1</divisions>
                                        <staves>2</staves>
                                        <key><fifths>0</fifths></key>
                                        <time><beats>4</beats><beat-type>4</beat-type></time>
                                        <clef number="1"><sign>G</sign><line>2</line></clef>
                                        <clef number="2"><sign>F</sign><line>4</line></clef>
                                    </attributes>
                                    <note><pitch><step>C</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>1</staff></note>
                                    <note><pitch><step>G</step><octave>2</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>2</staff></note>
                                    <note><pitch><step>E</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>1</staff></note>
                                    <note><pitch><step>C</step><octave>3</octave></pitch><duration>2</duration><voice>1</voice><type>half</type><staff>2</staff></note>
                                </measure>
                                <measure number="2">
                                    <note><pitch><step>D</step><octave>5</octave></pitch><duration>4</duration><voice>1</voice><type>whole</type><staff>1</staff></note>
                                    <note><pitch><step>A</step><octave>2</octave></pitch><duration>4</duration><voice>1</voice><type>whole</type><staff>2</staff></note>
                                </measure>
                                <measure number="3">
                                    <attributes>
                                        <key><fifths>2</fifths></key>
                                        <time><beats>3</beats><beat-type>4</beat-type></time>
                                    </attributes>
                                    <note><pitch><step>F</step><alter>1</alter><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>
                                    <note><pitch><step>D</step><octave>3</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>2</staff></note>
                                    <note><pitch><step>G</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>
                                    <note><pitch><step>E</step><octave>3</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>2</staff></note>
                                    <note><pitch><step>A</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>
                                    <note><pitch><step>F</step><alter>1</alter><octave>3</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type><staff>2</staff></note>
                                </measure>
                            </part>
                        </score-partwise>
                        """.trimIndent()
                )

                assertFixtureGolden(
                        sheet = sheet,
                        widthPx = 980,
                        golden = "grandstaff/key_time_change_980",
                        tolerance = 1.2
                )
        }

        @Test
        fun `clair de lune remains stable in narrow portrait-like width`() {
                assertClairGolden(widthPx = 420, golden = "grandstaff/clair_full_420", tolerance = 1.8)
        }

        private fun assertClairGolden(widthPx: Int, golden: String, tolerance: Double) {
        val sample = resolveRepoSample("samples/Clair_de_lune_-_Claude_Debussy.mxl")
        assertTrue("Sample file should exist: ${sample.absolutePath}", sample.exists())

        val sheet = sample.inputStream().use { MusicXMLParser().parse(it) }

        assertFixtureGolden(sheet = sheet, widthPx = widthPx, golden = golden, tolerance = tolerance)
    }

    private fun assertFixtureGolden(sheet: dev.pola.vexflow.parser.MusicSheet, widthPx: Int, golden: String, tolerance: Double) {
        val measures = MusicSheetToVF.convert(
            sheet = sheet,
            startX = 20f,
            startY = 70f,
            staveWidth = (widthPx - 40).toFloat()
        )

        val bitmap = VisualRenderHarness.renderMeasuresToBitmap(
            measures = measures,
            widthPx = widthPx
        )
        VisualGoldenAssert.assertMatchesGolden(
            goldenName = golden,
            actual = bitmap,
            tolerancePercent = tolerance
        )
    }

    private fun parseFixture(xml: String) =
        xml.byteInputStream(StandardCharsets.UTF_8).use { MusicXMLParser().parse(it) }

    private fun resolveRepoSample(pathFromAndroidRoot: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        val cwd = File(userDir).canonicalFile
        val candidates = listOf(
            File(cwd, "../$pathFromAndroidRoot"),
            File(cwd, "../../$pathFromAndroidRoot"),
            File(cwd, pathFromAndroidRoot),
            File(cwd, "android/$pathFromAndroidRoot")
        ).map { it.canonicalFile }

        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
