package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFSlurTest {

    private fun quarter(key: String, x: Float, stave: VFStave): VFStaveNote {
        val note = VFStaveNote(VFStaveNoteStruct(keys = listOf(key), duration = "4", glyphFontScale = 40f))
        note.setStave(stave)
        note.x = x
        return note
    }

    @Test
    fun `draw does not crash with two notes set`() {
        val stave = VFStave(0f, 100f, 300f)
        val from = quarter("e/4", 80f, stave)
        val to = quarter("f/4", 150f, stave)
        val slur = VFSlur(fromNote = from, toNote = to)

        slur.draw(RecordingContext())
    }

    @Test
    fun `isPartial true when fromNote is null`() {
        val slur = VFSlur(fromNote = null, toNote = null)
        assertTrue(slur.isPartial())
    }
}
