package dev.pola.vexflow.model

import android.graphics.RectF
import dev.pola.vexflow.core.RecordingContext
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFStaveNoteDottedRenderTest {

    private class RestDotAlignmentContext : VexRenderingContext() {
        data class GlyphCall(val codepoint: Int, val x: Float, val y: Float, val size: Float)

        val glyphCalls = mutableListOf<GlyphCall>()

        override fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
            glyphCalls += GlyphCall(codepoint, x, y, sizePx)
        }

        override fun measureSmuflGlyphBounds(codepoint: Int, sizePx: Float): RectF {
            return when (codepoint) {
                VFTables.GLYPH_REST_32ND,
                VFTables.GLYPH_REST_64TH -> RectF(-4f, -17f, 6f, 10f)

                VFTables.GLYPH_REST_128TH,
                VFTables.GLYPH_REST_256TH -> RectF(-4f, -27.5f, 6f, 12f)

                VFTables.GLYPH_AUGMENTATION_DOT -> RectF(-2f, -2f, 2f, 2f)
                else -> RectF(-4f, -10f, 6f, 10f)
            }
        }
    }

    @Test
    fun `dotted rest draws augmentation dot glyph`() {
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = 10f)
        )
        val rest = VFStaveNote(VFStaveNoteStruct(keys = listOf("b/4"), duration = "4dr", glyphFontScale = 40f))
        rest.setStave(stave)
        rest.x = 120f
        val ctx = RecordingContext()

        rest.draw(ctx)

        assertTrue(ctx.glyphCalls.any { it.codepoint == VFTables.GLYPH_AUGMENTATION_DOT })
    }

    @Test
    fun `dotted note draws augmentation dot glyph`() {
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = 10f)
        )
        val dotted = VFStaveNote(VFStaveNoteStruct(keys = listOf("c/4"), duration = "4d", glyphFontScale = 40f))
        dotted.setStave(stave)
        dotted.x = 120f
        val ctx = RecordingContext()

        dotted.draw(ctx)

        assertTrue(ctx.glyphCalls.any { it.codepoint == VFTables.GLYPH_AUGMENTATION_DOT })
    }

    @Test
    fun `dotted short rests align dots by rest shape`() {
        val stave = VFStave(
            x = 0f,
            y = 80f,
            width = 400f,
            options = VFStaveOptions(spacingBetweenLinesPx = 10f)
        )

        fun dottedRestDotY(duration: String): Float {
            val ctx = RestDotAlignmentContext()
            val rest = VFStaveNote(VFStaveNoteStruct(keys = listOf("b/4"), duration = duration, glyphFontScale = 40f))
            rest.setStave(stave)
            rest.x = 120f
            rest.draw(ctx)
            return ctx.glyphCalls.last { it.codepoint == VFTables.GLYPH_AUGMENTATION_DOT }.y
        }

        val dot32Y = dottedRestDotY("32dr")
        val dot128Y = dottedRestDotY("128dr")

        assertEquals(stave.getYForNote(1), dot32Y, 0.01f)
        assertEquals(stave.getYForNote(-1), dot128Y, 0.01f)
        assertTrue(dot128Y < dot32Y)
    }
}
