package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFBeamTest {

    private fun eighthNote(key: String, x: Float): VFStaveNote {
        val stave = VFStave(0f, 100f, 400f)
        val note = VFStaveNote(VFStaveNoteStruct(listOf(key), "8", 40f))
        note.setStave(stave)
        note.x = x
        return note
    }

    @Test
    fun `constructor marks all notes as beamed`() {
        val notes = listOf(eighthNote("c/5", 50f), eighthNote("d/5", 100f))
        VFBeam(notes)
        notes.forEach { assertTrue(it.isBeamed()) }
    }

    @Test
    fun `draw does not crash`() {
        val stave = VFStave(0f, 100f, 400f)
        val ctx = RecordingContext()
        val notes = listOf(
            eighthNote("c/5", 50f),
            eighthNote("d/5", 100f),
            eighthNote("e/5", 150f),
            eighthNote("f/5", 200f)
        )
        VFBeam(notes).draw(ctx, stave)
        assertTrue("Expected at least 4 stem strokes", ctx.strokeCalls.size >= 4)
    }

    @Test
    fun `single note beam draws nothing`() {
        val stave = VFStave(0f, 100f, 400f)
        val ctx = RecordingContext()
        VFBeam(listOf(eighthNote("c/5", 50f))).draw(ctx, stave)
        assertEquals(0, ctx.strokeCalls.size)
    }

    @Test
    fun `16th notes get secondary beam (more fill calls than 8th notes)`() {
        val stave = VFStave(0f, 100f, 400f)

        fun sixteenth(key: String, x: Float): VFStaveNote {
            val n = VFStaveNote(VFStaveNoteStruct(listOf(key), "16", 40f))
            n.setStave(stave)
            n.x = x
            return n
        }

        val ctx8th = RecordingContext()
        VFBeam(listOf(eighthNote("c/5", 50f), eighthNote("d/5", 100f))).draw(ctx8th, stave)

        val ctx16th = RecordingContext()
        VFBeam(listOf(sixteenth("c/5", 50f), sixteenth("d/5", 100f))).draw(ctx16th, stave)

        assertTrue(ctx16th.strokeCalls.size >= 2)
    }
}
