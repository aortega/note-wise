package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFClefTest {

    @Test
    fun `treble clef type parsed correctly`() {
        val c = VFClef("treble", "default", null)
        assertEquals(VFClef.ClefType.TREBLE, c.type)
    }

    @Test
    fun `bass clef type parsed correctly`() {
        assertEquals(VFClef.ClefType.BASS, VFClef("bass").type)
    }

    @Test
    fun `default size is 40`() {
        assertEquals(40f, VFClef("treble", "default", null).sizePx)
    }

    @Test
    fun `small size is 28`() {
        assertEquals(28f, VFClef("treble", "small", null).sizePx)
    }

    @Test
    fun `unknown clef defaults to treble`() {
        assertEquals(VFClef.ClefType.TREBLE, VFClef("unknown").type)
    }

    @Test
    fun `treble clef width is positive`() {
        assertTrue(VFClef("treble").width > 0f)
    }

    @Test
    fun `draw emits a glyph call`() {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 300f)
        val clef = VFClef("treble")
        clef.x = 10f

        clef.draw(stave, ctx)

        assertTrue(ctx.glyphCalls.isNotEmpty())
        assertEquals(clef.type.glyphCodepoint, ctx.glyphCalls.first().codepoint)
    }
}
