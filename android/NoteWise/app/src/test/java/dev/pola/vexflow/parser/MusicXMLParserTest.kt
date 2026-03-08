package dev.pola.vexflow.parser

import org.junit.Assert.assertEquals
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
    fun `MusicSheetToVF durationToVF quarter note`() {
        assertEquals("4", MusicSheetToVF.durationToVF(1, 1))
        assertEquals("4", MusicSheetToVF.durationToVF(2, 2))
    }

    @Test
    fun `MusicSheetToVF durationToVF dotted quarter`() {
        assertEquals("4d", MusicSheetToVF.durationToVF(6, 4))
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
}
