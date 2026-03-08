package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import dev.pola.vexflow.model.VFTables
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFKeySignatureTest {

    @Test
    fun `C major has no accidentals`() {
        val ks = VFKeySignature("C")
        assertTrue(ks.isEmpty)
        assertEquals(0, ks.accidentalCount)
    }

    @Test
    fun `G major has one sharp`() {
        val ks = VFKeySignature("G")
        assertEquals(1, ks.accidentalCount)
        assertTrue(ks.width > 0f)
    }

    @Test
    fun `Bb major has two flats`() {
        val ks = VFKeySignature("Bb")
        assertEquals(2, ks.accidentalCount)
    }

    @Test
    fun `unknown key falls back to no accidentals`() {
        val ks = VFKeySignature("XyZ")
        assertTrue(ks.isEmpty)
    }

    @Test
    fun `draw G major emits a sharp glyph`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ks = VFKeySignature("G")
        ks.x = 40f

        ks.draw(stave, ctx)

        assertEquals(1, ctx.glyphCalls.size)
        assertEquals(VFTables.GLYPH_ACCIDENTAL_SHARP, ctx.glyphCalls.first().codepoint)
    }

    @Test
    fun `draw FIFTHS-11 emits 7 flats then 4 double flats`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ks = VFKeySignature("FIFTHS-11")
        ks.x = 40f

        ks.draw(stave, ctx)

        assertEquals(11, ctx.glyphCalls.size)

        val codepoints = ctx.glyphCalls.map { it.codepoint }
        assertEquals(
            List(7) { VFTables.GLYPH_ACCIDENTAL_FLAT } +
                List(4) { VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT },
            codepoints
        )
    }
}
