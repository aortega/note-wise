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
class VFTieTest {

    private fun quarter(key: String, x: Float, stave: VFStave): VFStaveNote {
        val note = VFStaveNote(VFStaveNoteStruct(keys = listOf(key), duration = "4", glyphFontScale = 40f))
        note.setStave(stave)
        note.x = x
        return note
    }

    @Test
    fun `isPartial true when firstNote is null`() {
        val tie = VFTie(VFTieNotes(firstNote = null, lastNote = null))
        assertTrue(tie.isPartial())
    }

    @Test
    fun `draw does not crash with two notes set`() {
        val stave = VFStave(0f, 100f, 300f)
        val first = quarter("c/4", 80f, stave)
        val last = quarter("c/4", 150f, stave)
        val tie = VFTie(VFTieNotes(firstNote = first, lastNote = last))

        tie.draw(RecordingContext())
    }
}
