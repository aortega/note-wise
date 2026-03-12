package dev.pola.vexflow.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.elements.VFAccidental

class VFStaveNoteTest {

    private fun note(keys: List<String>, dur: String) =
        VFStaveNote(VFStaveNoteStruct(keys = keys, duration = dur, glyphFontScale = 40f))

    @Test
    fun `quarter note duration parses correctly`() {
        val n = note(listOf("c/4"), "4")
        assertEquals(VFFraction.of(1, 4), n.duration)
        assertFalse(n.isRest)
    }

    @Test
    fun `rest note isRest is true`() {
        val n = note(listOf("b/4"), "4r")
        assertTrue(n.isRest)
    }


    @Test
    fun `whole note is not beamed and has no flag`() {
        val n = note(listOf("c/4"), "1")
        assertFalse(n.isBeamed())
        assertTrue(n.duration >= VFFraction.of(1, 1))
    }

    @Test
    fun `pitchToNoteLineIndex treble G4 is 6`() {
        assertEquals(6, VFStaveNote.pitchToNoteLineIndex("g/4", null))
    }

    @Test
    fun `pitchToNoteLineIndex treble C5 is 3`() {
        assertEquals(3, VFStaveNote.pitchToNoteLineIndex("c/5", null))
    }

    @Test
    fun `pitchToNoteLineIndex treble E4 is line 8`() {
        assertEquals(8, VFStaveNote.pitchToNoteLineIndex("e/4", null))
    }

    @Test
    fun `sharp accidental does not change line index`() {
        val lineNatural = VFStaveNote.pitchToNoteLineIndex("f/4", null)
        val lineSharp = VFStaveNote.pitchToNoteLineIndex("f#/4", null)
        assertEquals(lineNatural, lineSharp)
    }

    @Test
    fun `stem direction defaults up for notes below middle`() {
        val n = note(listOf("e/4"), "4")
        assertEquals(VFStaveNoteStruct.STEM_UP, n.getStemDirection())
    }

    @Test
    fun `setBeamed suppresses flag drawing check`() {
        val n = note(listOf("c/4"), "8")
        assertFalse(n.isBeamed())
        n.setBeamed(true)
        assertTrue(n.isBeamed())
    }

    @Test
    fun `getMetrics returns non-zero width`() {
        val m = note(listOf("c/4"), "4").getMetrics()
        assertTrue(m.width > 0f)
        assertTrue(m.totalLeftPx > 0f)
        assertTrue(m.totalRightPx > 0f)
    }

    @Test
    fun `unknown duration string throws`() {
        assertThrows(IllegalStateException::class.java) {
            note(listOf("c/4"), "xyz")
        }
    }

    @Test
    fun `accidental extra left spacing is close to staff-space scale`() {
        val staffSpacing = 8f
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = staffSpacing)
        )
        val n = VFStaveNote(
            VFStaveNoteStruct(keys = listOf("f#/4"), duration = "4", glyphFontScale = staffSpacing * 4f)
        )
        n.setStave(stave)

        val metrics = n.getMetrics()
        val accidentalExtraLeft = metrics.totalLeftPx - metrics.totalRightPx

        // Keep accidental gap compact and staff-space-based (roughly <= 2 staff spaces).
        assertTrue(accidentalExtraLeft <= staffSpacing * 2f)
        assertTrue(accidentalExtraLeft >= staffSpacing * 0.4f)
    }

    @Test
    fun `microtone accidental contributes left spacing`() {
        val staffSpacing = 8f
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = staffSpacing)
        )
        val n = VFStaveNote(
            VFStaveNoteStruct(keys = listOf("eqs/4"), duration = "4", glyphFontScale = staffSpacing * 4f)
        )
        n.setStave(stave)

        val metrics = n.getMetrics()
        assertTrue(metrics.totalLeftPx > metrics.totalRightPx)
    }

    @Test
    fun `decorated accidental reserves extra left spacing`() {
        val staffSpacing = 8f
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = staffSpacing)
        )

        val plain = VFStaveNote(
            VFStaveNoteStruct(keys = listOf("f#/4"), duration = "4", glyphFontScale = staffSpacing * 4f)
        )
        plain.setStave(stave)

        val decorated = VFStaveNote(
            VFStaveNoteStruct(
                keys = listOf("f#/4"),
                duration = "4",
                glyphFontScale = staffSpacing * 4f,
                accidentalDisplayOptions = listOf(
                    VFAccidental.DisplayOptions(parenthesized = true, bracketed = true)
                )
            )
        )
        decorated.setStave(stave)

        assertTrue(decorated.getMetrics().totalLeftPx > plain.getMetrics().totalLeftPx)
    }

    @Test
    fun `dotted duration reserves extra right spacing`() {
        val plain = note(listOf("c/4"), "4")
        val dotted = note(listOf("c/4"), "4d")

        assertTrue(dotted.getMetrics().totalRightPx > plain.getMetrics().totalRightPx)
    }
}
