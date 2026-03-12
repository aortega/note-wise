package dev.pola.vexflow.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MusicXMLParserTest {

    private fun xmlSheet(body: String): MusicSheet {
                val xml = """<?xml version="1.0" encoding="UTF-8"?>
<score-partwise version="3.1">
  <work><work-title>Test</work-title></work>
    <part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list>
    <part id="P1">$body</part>
</score-partwise>"""
        return MusicXMLParser().parse(xml.byteInputStream())
    }

    @Test
    fun `title is parsed`() {
        val sheet = xmlSheet("<measure number='1'><attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>")
        assertEquals("Test", sheet.title)
    }

    @Test
    fun `empty measure produces zero notes`() {
        val sheet = xmlSheet("<measure number='1'><attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>")
        assertEquals(0, sheet.parts[0].measures[0].notes.size)
    }

    @Test
    fun `quarter note is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>
</measure>"""
        val measure = xmlSheet(xml).parts[0].measures[0]
        assertEquals(1, measure.notes.size)
        val note = measure.notes[0] as NoteData
        assertEquals("C", note.pitch.step)
        assertEquals(4, note.pitch.octave)
        assertEquals(1, note.duration)
    }

    @Test
    fun `rest is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><rest/><duration>1</duration><voice>1</voice></note>
</measure>"""
        val note = xmlSheet(xml).parts[0].measures[0].notes[0]
        assertTrue(note is RestData)
    }

    @Test
    fun `rest display step and octave are parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><rest><display-step>E</display-step><display-octave>4</display-octave></rest><duration>1</duration><voice>1</voice></note>
</measure>"""
        val rest = xmlSheet(xml).parts[0].measures[0].notes[0] as RestData
        assertEquals("E", rest.displayStep)
        assertEquals(4, rest.displayOctave)
    }

    @Test
    fun `rest measure no attribute is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>3</beats><beat-type>2</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><rest measure='no'/><duration>6</duration><voice>1</voice></note>
</measure>"""
        val rest = xmlSheet(xml).parts[0].measures[0].notes[0] as RestData
        assertEquals(false, rest.measureRest)
    }

    @Test
    fun `sharp accidental is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>F</step><octave>5</octave><alter>1</alter></pitch><duration>1</duration><voice>1</voice><accidental>sharp</accidental></note>
</measure>"""
        val note = xmlSheet(xml).parts[0].measures[0].notes[0] as NoteData
        assertEquals("#", note.accidental)
        assertEquals(1f, note.pitch.alter)
    }

    @Test
    fun `key signature with 2 sharps is parsed`() {
        val xml = """<measure number='1'><attributes><divisions>1</divisions><key><fifths>2</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>"""
        val attrs = xmlSheet(xml).parts[0].measures[0].attributes
        assertEquals(2, attrs.keyFifths)
        assertEquals("major", attrs.keyMode)
    }

        @Test
        fun `multiple rest is measure scoped and does not carry forward`() {
                val xml = """
<measure number='1'>
    <attributes>
        <divisions>1</divisions>
        <key><fifths>0</fifths></key>
        <time><beats>2</beats><beat-type>2</beat-type></time>
        <clef><sign>G</sign></clef>
        <measure-style><multiple-rest>2</multiple-rest></measure-style>
    </attributes>
    <note><rest/><duration>2</duration><voice>1</voice><type>whole</type></note>
</measure>
<measure number='2'>
    <note><rest/><duration>2</duration><voice>1</voice><type>whole</type></note>
</measure>
<measure number='3'>
    <note><rest/><duration>2</duration><voice>1</voice><type>whole</type></note>
</measure>
""".trimIndent()

                val measures = xmlSheet(xml).parts[0].measures
                assertEquals(2, measures[0].attributes.multipleRestCount)
                assertEquals(0, measures[1].attributes.multipleRestCount)
                assertEquals(0, measures[2].attributes.multipleRestCount)
        }

    @Test
    fun `MusicSheetToVF durationToVF quarter note`() {
        assertEquals("4", MusicSheetToVF.durationToVF(1, 1))
        assertEquals("4", MusicSheetToVF.durationToVF(2, 2))
    }

    @Test
    fun `MusicSheetToVF durationToVF dotted quarter`() {
        assertEquals("4d", MusicSheetToVF.durationToVF(6, 4))
    }

    @Test
    fun `MusicSheetToVF durationToVF supports very short rests`() {
        assertEquals("64", MusicSheetToVF.durationToVF(32, 512))
        assertEquals("128", MusicSheetToVF.durationToVF(16, 512))
        assertEquals("256", MusicSheetToVF.durationToVF(8, 512))
        assertEquals("512", MusicSheetToVF.durationToVF(4, 512))
        assertEquals("1024", MusicSheetToVF.durationToVF(2, 512))
        assertEquals("1024d", MusicSheetToVF.durationToVF(3, 512))
    }

    @Test
    fun `MusicSheetToVF fifthsToKeySpec 2 sharps major`() {
        assertEquals("D", MusicSheetToVF.fifthsToKeySpec(2, "major"))
    }

    @Test
    fun `MusicSheetToVF fifthsToKeySpec extreme flats uses FIFTHS token`() {
        assertEquals("FIFTHS-11", MusicSheetToVF.fifthsToKeySpec(-11, "major"))
    }

    @Test
    fun `MusicSheetToVF pitchToKey F sharp 5`() {
        assertEquals("f#/5", MusicSheetToVF.pitchToKey(Pitch("F", 5, 1f), "#", 0))
    }

    @Test
    fun `MusicSheetToVF pitchToKey infers natural C in FIFTHS-11`() {
        assertEquals("cn/4", MusicSheetToVF.pitchToKey(Pitch("C", 4, 0f), null, -11))
    }

    @Test
    fun `MusicSheetToVF pitchToKey infers microtone accidental suffixes`() {
        assertEquals("cdb/4", MusicSheetToVF.pitchToKey(Pitch("C", 4, -1.5f), null, 0))
        assertEquals("dqb/4", MusicSheetToVF.pitchToKey(Pitch("D", 4, -0.5f), null, 0))
        assertEquals("eqs/4", MusicSheetToVF.pitchToKey(Pitch("E", 4, 0.5f), null, 0))
        assertEquals("f#t/4", MusicSheetToVF.pitchToKey(Pitch("F", 4, 1.5f), null, 0))
    }

    @Test
    fun `microtone explicit accidental tags are parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>D</step><octave>4</octave><alter>-0.5</alter></pitch><duration>1</duration><voice>1</voice><accidental>quarter-flat</accidental></note>
<note><pitch><step>F</step><octave>4</octave><alter>1.5</alter></pitch><duration>1</duration><voice>1</voice><accidental>three-quarters-sharp</accidental></note>
</measure>"""
        val notes = xmlSheet(xml).parts[0].measures[0].notes

        assertEquals("qb", (notes[0] as NoteData).accidental)
        assertEquals("#t", (notes[1] as NoteData).accidental)
    }

    @Test
    fun `accidental display attributes are parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>D</step><octave>4</octave><alter>-1</alter></pitch><duration>1</duration><voice>1</voice><accidental cautionary='yes'>flat</accidental></note>
<note><pitch><step>D</step><octave>4</octave><alter>-1</alter></pitch><duration>1</duration><voice>1</voice><accidental editorial='yes'>flat</accidental></note>
<note><pitch><step>C</step><octave>4</octave><alter>1</alter></pitch><duration>1</duration><voice>1</voice><accidental cautionary='yes' parentheses='no'>sharp</accidental></note>
<note><pitch><step>D</step><octave>4</octave><alter>-2</alter></pitch><duration>1</duration><voice>1</voice><accidental editorial='yes' cautionary='yes'>flat-flat</accidental></note>
<note><pitch><step>C</step><octave>4</octave><alter>2</alter></pitch><duration>1</duration><voice>1</voice><accidental bracket='yes' parentheses='yes'>double-sharp</accidental></note>
</measure>"""

        val notes = xmlSheet(xml).parts[0].measures[0].notes
        
        // cautionary alone -> parentheses by default
        val cautionary = notes[0] as NoteData
        assertTrue(cautionary.accidentalCautionary)
        assertFalse(cautionary.accidentalEditorial)
        assertTrue(cautionary.accidentalParenthesized)
        assertFalse(cautionary.accidentalBracketed)

        // editorial alone -> brackets by default
        val editorial = notes[1] as NoteData
        assertFalse(editorial.accidentalCautionary)
        assertTrue(editorial.accidentalEditorial)
        assertFalse(editorial.accidentalParenthesized)
        assertTrue(editorial.accidentalBracketed)

        // explicit parentheses='no' overrides default
        val noParens = notes[2] as NoteData
        assertTrue(noParens.accidentalCautionary)
        assertFalse(noParens.accidentalParenthesized)
        assertFalse(noParens.accidentalBracketed)

        // both editorial and cautionary -> brackets only (brackets suppress parentheses)
        val both = notes[3] as NoteData
        assertTrue(both.accidentalCautionary)
        assertTrue(both.accidentalEditorial)
        assertFalse(both.accidentalParenthesized) // suppressed by bracket
        assertTrue(both.accidentalBracketed)

        // explicit bracket always suppresses parentheses
        val bracketed = notes[4] as NoteData
        assertTrue(bracketed.accidentalBracketed)
        assertFalse(bracketed.accidentalParenthesized) // brackets suppress parens
    }
}
