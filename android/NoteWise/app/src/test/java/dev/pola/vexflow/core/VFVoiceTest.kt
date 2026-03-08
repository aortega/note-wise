package dev.pola.vexflow.core

import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VFVoiceTest {

    private fun note(key: String, duration: String): VFStaveNote =
        VFStaveNote(VFStaveNoteStruct(keys = listOf(key), duration = duration, glyphFontScale = 40f))

    @Test
    fun `addTickables adds all notes`() {
        val voice = VFVoice("4/4")
        val notes = listOf(note("c/4", "4"), note("d/4", "4"), note("e/4", "4"))

        voice.addTickables(notes)

        assertEquals(3, voice.tickables.size)
    }

    @Test
    fun `getTotalTicks sums durations correctly`() {
        val voice = VFVoice("4/4")
        voice.addTickables(listOf(note("c/4", "4"), note("d/4", "4")))

        assertEquals(VFFraction.of(1, 2), voice.getTotalTicks())
    }

    @Test
    fun `clear empties tickables`() {
        val voice = VFVoice("4/4")
        voice.addTickables(listOf(note("c/4", "4"), note("d/4", "8")))

        voice.clear()

        assertTrue(voice.tickables.isEmpty())
        assertEquals(VFFraction.ZERO, voice.getTotalTicks())
    }

    @Test
    fun `parseTimeSpec 3 over 4 returns 3 over 4`() {
        assertEquals(VFFraction.of(3, 4), VFVoice.parseTimeSpec("3/4"))
    }
}
