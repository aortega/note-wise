package dev.pola.vexflow.core

import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VFTickContextTest {

    private fun note(duration: String): VFStaveNote =
        VFStaveNote(VFStaveNoteStruct(keys = listOf("c/4"), duration = duration, glyphFontScale = 40f))

    @Test
    fun `addTickable propagates tick context to note`() {
        val tickContext = VFTickContext(0)
        val n = note("4")

        tickContext.addTickable(n, voiceIndex = 0)

        val field = VFStaveNote::class.java.getDeclaredField("tickContext")
        field.isAccessible = true
        assertSame(tickContext, field.get(n))
    }

    @Test
    fun `setX propagates to all notes`() {
        val tickContext = VFTickContext(0)
        val n1 = note("4")
        val n2 = note("8")
        tickContext.addTickable(n1, voiceIndex = 0)
        tickContext.addTickable(n2, voiceIndex = 1)

        tickContext.x = 123f

        assertEquals(123f, n1.x, 0.001f)
        assertEquals(123f, n2.x, 0.001f)
    }

    @Test
    fun `preFormat computes non-zero width`() {
        val tickContext = VFTickContext(0)
        tickContext.addTickable(note("4"), voiceIndex = 0)

        tickContext.preFormat()

        assertTrue(tickContext.width > 0f)
    }

    @Test
    fun `getMaxDuration returns largest duration`() {
        val tickContext = VFTickContext(0)
        tickContext.addTickable(note("8"), voiceIndex = 0)
        tickContext.addTickable(note("2"), voiceIndex = 1)

        val max = tickContext.getMaxDuration()

        assertEquals(VFFraction.of(1, 2), max)
    }
}
