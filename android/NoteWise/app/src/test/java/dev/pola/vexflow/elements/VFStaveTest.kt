package dev.pola.vexflow.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.core.VexRenderingContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFStaveTest {

    private fun stave(x: Float = 0f, y: Float = 100f, width: Float = 400f) =
        VFStave(x, y, width)

    @Test
    fun `getYForLine line 0 returns y`() {
        assertEquals(100f, stave(y = 100f).getYForLine(0f))
    }

    @Test
    fun `getYForLine line 4 returns y plus 4 spacings`() {
        val s = stave(y = 100f)
        assertEquals(100f + 4 * VFMetrics.DEFAULT_LINE_SPACING, s.getYForLine(4f))
    }

    @Test
    fun `getYForNote half-space resolution`() {
        val s = stave(y = 100f)
        assertEquals(100f, s.getYForNote(0))
        assertEquals(100f + VFMetrics.DEFAULT_LINE_SPACING / 2f, s.getYForNote(1))
        assertEquals(100f + VFMetrics.DEFAULT_LINE_SPACING, s.getYForNote(2))
    }

    @Test
    fun `getNoteStartX without modifiers`() {
        val s = stave(x = 50f)
        val expectedInset = s.spacingBetweenLines * VFMetrics.STAVE_LEFT_PADDING_SPACES
        assertEquals(50f + expectedInset, s.getNoteStartX())
    }

    @Test
    fun `getNoteStartX with clef adds clef width and padding`() {
        val s = stave(x = 0f)
        val clef = VFClef("treble", "default", null)
        s.clef = clef
        val expected = (s.spacingBetweenLines * VFMetrics.STAVE_LEFT_PADDING_SPACES) +
            clef.widthForStaffSpacing(s.spacingBetweenLines) +
            VFMetrics.clefPaddingPx(s.spacingBetweenLines)
        assertEquals(expected, s.getNoteStartX(), 0.5f)
    }

    @Test
    fun `draw does not crash with recording context`() {
        val ctx = RecordingContextForStaveTest()
        val s = stave()
        s.draw(ctx)
        assertTrue("Expected staff lines to be stroked", ctx.strokeCalls > 0)
    }

    @Test
    fun `modifier ordering keeps key signature between clef and time signature`() {
        val s = stave(x = 20f)
        s.clef = VFClef("treble", "default", null)
        s.keySignature = VFKeySignature("D")
        s.timeSignature = VFTimeSignature("4/4")

        val start = s.x + (s.spacingBetweenLines * VFMetrics.STAVE_LEFT_PADDING_SPACES)
        val clefStart = start
        val keyStart =
            clefStart +
                s.clef!!.widthForStaffSpacing(s.spacingBetweenLines) +
                VFMetrics.clefPaddingPx(s.spacingBetweenLines)
        val timeStart =
            keyStart +
                s.keySignature!!.widthForStaffSpacing(s.spacingBetweenLines) +
                VFMetrics.keySignaturePaddingPx(s.spacingBetweenLines)

        assertTrue(keyStart > clefStart)
        assertTrue(timeStart > keyStart)
        assertTrue(s.getNoteStartX() > timeStart)
    }
}

private class RecordingContextForStaveTest : VexRenderingContext() {
    var strokeCalls: Int = 0

    override fun stroke() {
        strokeCalls += 1
    }
}
