package dev.pola.vexflow.model

import android.graphics.Bitmap
import android.graphics.Canvas
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFMicrotoneAccidentalRenderTest {

    @Test
    fun `microtone accidentals emit glyph boxes when drawn`() {
        val bitmap = Bitmap.createBitmap(512, 160, Bitmap.Config.ARGB_8888)
        val ctx = VexRenderingContext().apply {
            canvas = Canvas(bitmap)
            debugCollectGlyphBoxes = true
        }
        val stave = VFStave(
            x = 20f,
            y = 60f,
            width = 420f,
            options = VFStaveOptions(spacingBetweenLinesPx = 10f)
        )

        val notes = listOf(
            VFStaveNote(VFStaveNoteStruct(keys = listOf("cdb/4"), duration = "4", glyphFontScale = 40f)),
            VFStaveNote(VFStaveNoteStruct(keys = listOf("dqb/4"), duration = "4", glyphFontScale = 40f)),
            VFStaveNote(VFStaveNoteStruct(keys = listOf("eqs/4"), duration = "4", glyphFontScale = 40f)),
            VFStaveNote(VFStaveNoteStruct(keys = listOf("f#t/4"), duration = "4", glyphFontScale = 40f))
        )

        notes.forEachIndexed { index, note ->
            note.x = 90f + index * 90f
            note.setStave(stave)
            note.draw(ctx)
        }

        val codepoints = ctx.consumeDebugGlyphBoxes().map { it.codepoint }.toSet()

        assertTrue(codepoints.contains(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT))
        assertTrue(codepoints.contains(VFTables.GLYPH_ACCIDENTAL_QUARTER_FLAT))
        assertTrue(codepoints.contains(VFTables.GLYPH_ACCIDENTAL_QUARTER_SHARP))
        assertTrue(codepoints.contains(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP))
    }

    @Test
    fun `microtone accidentals keep horizontal clearance from noteheads`() {
        val cases = listOf(
            "cdb/4" to VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT,
            "dqb/4" to VFTables.GLYPH_ACCIDENTAL_QUARTER_FLAT,
            "eqs/4" to VFTables.GLYPH_ACCIDENTAL_QUARTER_SHARP,
            "f#t/4" to VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP
        )

        for ((key, accidentalCodepoint) in cases) {
            val bitmap = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888)
            val ctx = VexRenderingContext().apply {
                canvas = Canvas(bitmap)
                debugCollectGlyphBoxes = true
            }
            val stave = VFStave(
                x = 20f,
                y = 60f,
                width = 260f,
                options = VFStaveOptions(spacingBetweenLinesPx = 10f)
            )
            val note = VFStaveNote(
                VFStaveNoteStruct(keys = listOf(key), duration = "4", glyphFontScale = 40f)
            )
            note.x = 140f
            note.setStave(stave)
            note.draw(ctx)

            val boxes = ctx.consumeDebugGlyphBoxes()
            val accidentalBox = boxes.firstOrNull { it.codepoint == accidentalCodepoint }
            val noteheadBox = boxes.firstOrNull { it.codepoint == VFTables.GLYPH_NOTE_HEAD_QUARTER }

            assertNotNull("Missing accidental glyph box for $key", accidentalBox)
            assertNotNull("Missing notehead glyph box for $key", noteheadBox)

            // Keep at least sub-pixel separation to avoid visible collisions.
            assertTrue(
                "Accidental overlaps notehead for $key",
                accidentalBox!!.right <= noteheadBox!!.left - 0.5f
            )
        }
    }
}