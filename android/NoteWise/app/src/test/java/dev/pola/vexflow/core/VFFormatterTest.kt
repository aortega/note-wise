package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VFFormatterTest {

    private fun makeDependencies(): Triple<VFFormatter, VFVoice, VFStave> {
        val stave = VFStave(0f, 100f, 500f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("4/4").apply { setStave(stave) }
        return Triple(formatter, voice, stave)
    }

    private fun quarterNote(key: String) =
        VFStaveNote(VFStaveNoteStruct(keys = listOf(key), duration = "4", glyphFontScale = 40f))

    @Test
    fun `four quarter notes get increasing x positions`() {
        val (formatter, voice, stave) = makeDependencies()
        val notes = listOf("c/5", "d/5", "e/5", "f/5").map { quarterNote(it) }
        voice.addTickables(notes)

        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 400f)

        val xs = notes.map { it.x }
        for (i in 1 until xs.size) {
            assertTrue(xs[i] > xs[i - 1], "Note $i x=${xs[i]} should be > ${xs[i - 1]}")
        }
    }

    @Test
    fun `first note x is offset from startX for left glyph extents`() {
        val (formatter, voice, stave) = makeDependencies()
        val note = quarterNote("c/5")
        voice.addTickable(note)

        formatter.formatVoices(listOf(voice), stave, startX = 75f, justifyWidth = 300f)

        assertTrue(note.x > 75f)
    }

    @Test
    fun `minimum spacing enforced when justifyWidth is 0`() {
        val (formatter, voice, stave) = makeDependencies()
        val notes = listOf(quarterNote("c/5"), quarterNote("d/5"))
        voice.addTickables(notes)

        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 0f)

        val gap = notes[1].x - notes[0].x
        assertTrue(gap >= 10f, "Gap $gap should be >= minWidth 10f")
    }

    @Test
    fun `two voices at same beat share x position`() {
        val (formatter, _, stave) = makeDependencies()
        val voice1 = VFVoice("4/4").apply { setStave(stave) }
        val voice2 = VFVoice("4/4").apply { setStave(stave) }
        val note1 = quarterNote("e/5").also { voice1.addTickable(it) }
        val note2 = quarterNote("c/4").also { voice2.addTickable(it) }

        formatter.formatVoices(listOf(voice1, voice2), stave, startX = 50f, justifyWidth = 300f)

        assertEquals(note1.x, note2.x, 0.01f)
    }

    @Test
    fun `whole note gets same x as single context`() {
        val (formatter, voice, stave) = makeDependencies()
        val whole = VFStaveNote(VFStaveNoteStruct(listOf("c/4"), "1", 40f))
        voice.addTickable(whole)

        formatter.formatVoices(listOf(voice), stave, startX = 60f, justifyWidth = 400f)

        assertTrue(whole.x > 60f)
    }

    @Test
    fun `first note with accidental gets additional leading offset`() {
        val (formatter, voice, stave) = makeDependencies()
        val plain = VFStaveNote(VFStaveNoteStruct(keys = listOf("c/5"), duration = "4", glyphFontScale = 40f))
        val withAcc = VFStaveNote(VFStaveNoteStruct(keys = listOf("c#/5"), duration = "4", glyphFontScale = 40f))

        voice.clear()
        voice.addTickable(plain)
        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 300f)
        val plainX = plain.x

        voice.clear()
        voice.addTickable(withAcc)
        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 300f)
        val accidentalX = withAcc.x

        assertTrue(accidentalX > plainX + VFMetrics.signatureToNotesGapPx(stave.spacingBetweenLines) / 2f)
    }

    @Test
    fun `four quarter notes fit inside measure bounds`() {
        val stave = VFStave(0f, 100f, 130f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("4/4").apply { setStave(stave) }
        val notes = listOf("c/5", "d/5", "e/5", "f/5").map { quarterNote(it) }
        voice.addTickables(notes)

        formatter.formatVoices(listOf(voice), stave, startX = 10f, justifyWidth = 120f)

        val leftBound = stave.x
        val rightBound = stave.x + stave.width
        notes.forEachIndexed { i, note ->
            val m = note.getMetrics()
            assertTrue(note.x - m.totalLeftPx >= leftBound, "Note $i left overflow")
            assertTrue(note.x + m.totalRightPx <= rightBound, "Note $i right overflow")
        }
    }

    @Test
    fun `four quarter notes use alphaTab style spring anchors`() {
        val stave = VFStave(0f, 100f, 130f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("4/4").apply { setStave(stave) }
        val notes = listOf("g/2", "a/2", "b/2", "c/3").map { quarterNote(it) }
        voice.addTickables(notes)

        val startX = 10f
        formatter.formatVoices(listOf(voice), stave, startX = startX, justifyWidth = 120f)

        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val available = (stave.x + stave.width - startX - rightSafety).coerceAtLeast(0f)
        val expectedFirstX = startX + notes.first().getMetrics().totalLeftPx
        val expectedSpringWidth = (available - notes.first().getMetrics().totalLeftPx) / notes.size

        assertEquals(expectedFirstX, notes.first().x, 0.01f)
        for (i in 1 until notes.size) {
            assertEquals(expectedSpringWidth, notes[i].x - notes[i - 1].x, 0.01f)
        }
    }

    @Test
    fun `two quarter notes in 2-4 time use equal spring anchors`() {
        val stave = VFStave(0f, 100f, 130f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("2/4").apply { setStave(stave) }
        val notes = listOf("c/5", "d/5").map { quarterNote(it) }
        voice.addTickables(notes)

        val startX = 10f
        formatter.formatVoices(listOf(voice), stave, startX = startX, justifyWidth = 120f)

        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val available = (stave.x + stave.width - startX - rightSafety).coerceAtLeast(0f)
        val expectedFirstX = startX + notes.first().getMetrics().totalLeftPx
        val expectedSpringWidth = (available - notes.first().getMetrics().totalLeftPx) / notes.size

        assertEquals(expectedFirstX, notes.first().x, 0.01f)
        assertEquals(expectedFirstX + expectedSpringWidth, notes[1].x, 0.01f)
        // Second note should be at the midpoint of the available region
        assertTrue(notes[1].x > notes[0].x)
    }

    @Test
    fun `equal beat grid prevents overlap for dense contexts`() {
        val stave = VFStave(0f, 100f, 120f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("4/4").apply { setStave(stave) }
        val notes = List(11) {
            VFStaveNote(VFStaveNoteStruct(keys = listOf("b/4"), duration = "32r", glyphFontScale = 40f))
        }
        voice.addTickables(notes)

        formatter.formatVoices(listOf(voice), stave, startX = 10f, justifyWidth = 100f)

        for (i in 1 until notes.size) {
            val prev = notes[i - 1]
            val curr = notes[i]
            val prevMetrics = prev.getMetrics()
            val currMetrics = curr.getMetrics()
            val gap = curr.x - prev.x
            val required = prevMetrics.totalRightPx + currMetrics.totalLeftPx
            assertTrue(gap >= required - 0.01f, "Context $i overlap: gap=$gap required=$required")
        }
    }

    @Test
    fun `full measure rest is centered in note area`() {
        val stave = VFStave(0f, 100f, 300f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("2/2").apply { setStave(stave) }
        val rest = VFStaveNote(
            VFStaveNoteStruct(
                keys = listOf("g/4"),
                duration = "1r",
                glyphFontScale = 40f,
                measureRestCount = 1
            )
        )
        voice.addTickable(rest)

        val startX = stave.getNoteStartX()
        val justifyWidth = (stave.width - (startX - stave.x)).coerceAtLeast(0f)
        formatter.formatVoices(listOf(voice), stave, startX = startX, justifyWidth = justifyWidth)

        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val noteAreaEnd = stave.x + stave.width - rightSafety
        val expectedCenterX = (startX + noteAreaEnd) / 2f
        assertEquals(expectedCenterX, rest.x, 0.01f)
    }

    @Test
    fun `dense rest only contexts stay within note area`() {
        val stave = VFStave(0f, 100f, 300f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("2/2").apply { setStave(stave) }
        val durations = listOf("2r", "4r", "8r", "16r", "32r", "64r", "128r", "256r", "512r", "1024r", "1024r")
        val notes = durations.map { duration ->
            VFStaveNote(VFStaveNoteStruct(keys = listOf("b/4"), duration = duration, glyphFontScale = 40f))
        }
        voice.addTickables(notes)

        val startX = stave.getNoteStartX()
        val justifyWidth = (stave.width - (startX - stave.x)).coerceAtLeast(0f)
        formatter.formatVoices(listOf(voice), stave, startX = startX, justifyWidth = justifyWidth)

        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val noteAreaEnd = stave.x + stave.width - rightSafety
        notes.forEachIndexed { index, note ->
            val m = note.getMetrics()
            assertTrue(note.x - m.totalLeftPx >= startX - 0.01f, "Rest $index left overflow")
            assertTrue(note.x + m.totalRightPx <= noteAreaEnd + 0.01f, "Rest $index right overflow")
        }
    }

    @Test
    fun `rest only compression keeps duration weighted spacing`() {
        val stave = VFStave(0f, 100f, 220f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("2/2").apply { setStave(stave) }
        val durations = listOf("2r", "4r", "8r", "16r", "32r", "64r", "128r", "256r")
        val notes = durations.map { duration ->
            VFStaveNote(VFStaveNoteStruct(keys = listOf("b/4"), duration = duration, glyphFontScale = 40f))
        }
        voice.addTickables(notes)

        val startX = stave.getNoteStartX()
        val justifyWidth = (stave.width - (startX - stave.x)).coerceAtLeast(0f)
        formatter.formatVoices(listOf(voice), stave, startX = startX, justifyWidth = justifyWidth)

        val gaps = notes.zipWithNext { prev, curr -> curr.x - prev.x }
        assertTrue(gaps.first() > gaps.last() + 0.5f, "Expected earlier longer-duration gap to be larger: $gaps")
    }
}
