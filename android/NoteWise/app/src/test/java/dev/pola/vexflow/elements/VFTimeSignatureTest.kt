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
class VFTimeSignatureTest {

    @Test
    fun `common time draws common glyph`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ts = VFTimeSignature("C")
        ts.x = 70f

        ts.draw(stave, ctx)

        assertEquals(1, ctx.glyphCalls.size)
        assertEquals(VFTables.GLYPH_TIME_SIG_COMMON, ctx.glyphCalls.first().codepoint)
    }

    @Test
    fun `cut common time draws cut glyph`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ts = VFTimeSignature("C|")

        ts.draw(stave, ctx)

        assertEquals(1, ctx.glyphCalls.size)
        assertEquals(VFTables.GLYPH_TIME_SIG_CUT, ctx.glyphCalls.first().codepoint)
    }

    @Test
    fun `numeric 4 slash 4 draws two digits`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ts = VFTimeSignature("4/4")

        ts.draw(stave, ctx)

        assertEquals(2, ctx.glyphCalls.size)
        assertTrue(ctx.glyphCalls.all { it.codepoint == VFTables.GLYPH_TIME_SIG_4 })
    }

    @Test
    fun `invalid spec draws nothing`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val ts = VFTimeSignature("invalid")

        ts.draw(stave, ctx)

        assertEquals(0, ctx.glyphCalls.size)
    }

    @Test
    fun `width is positive for numeric and common`() {
        assertTrue(VFTimeSignature("3/4").width > 0f)
        assertTrue(VFTimeSignature("C").width > 0f)
    }
}
