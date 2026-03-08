package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import dev.pola.vexflow.parser.MusicSheetToVF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VFLineBreakerTest {

    @Test
    fun `layout wraps measures into multiple rows when width is limited`() {
        val measures = List(12) { buildMeasure(noteCount = 4) }

        val layout = VFLineBreaker.layout(
            measures = measures,
            systemWidth = 800f,
            startX = 20f,
            startY = 60f,
            systemSpacing = 90f
        )

        assertTrue("Expected line breaks for 12 measures", layout.rows.size >= 2)
        assertEquals(layout.rows.size, layout.systemY.size)
    }

    @Test
    fun `layout rows do not overlap measures`() {
        val measures = List(8) { buildMeasure(noteCount = 6) }
        val startX = 20f
        val systemWidth = 700f

        val layout = VFLineBreaker.layout(
            measures = measures,
            systemWidth = systemWidth,
            startX = startX,
            startY = 50f
        )

        layout.rows.forEach { row ->
            var previousRight = startX
            val firstMeasure = row.firstOrNull()
            firstMeasure?.staves?.forEach { staff ->
                assertTrue(staff.stave.clef != null)
            }
            row.forEach { rendered ->
                val stave = rendered.staves.first().stave
                assertTrue(stave.x + 0.01f >= previousRight)
                previousRight = stave.x + stave.width
            }
            assertTrue(previousRight <= startX + systemWidth + 1f)
        }
    }

    @Test
    fun `consecutive staves keep minimum 2x spacing between content`() {
        val measure = buildGrandStaffMeasureForOverlap()

        val layout = VFLineBreaker.layout(
            measures = listOf(measure),
            systemWidth = 600f,
            startX = 20f,
            startY = 50f,
            systemSpacing = 40f
        )

        val staves = layout.rows.first().first().staves.sortedBy { it.stave.y }
        val upper = staves[0]
        val lower = staves[1]
        val gap = (lower.stave.getTopLineTopY() - upper.stave.getBottomLineBottomY())

        assertTrue(gap >= 2f * upper.stave.spacingBetweenLines)
    }

    @Test
    fun `staff y layout is stable when row has mixed staff sets`() {
        val twoStaffMeasure = buildGrandStaffMeasureForOverlap()
        val upperOnlyMeasure = buildMeasure(noteCount = 4)

        val layout = VFLineBreaker.layout(
            measures = listOf(twoStaffMeasure, upperOnlyMeasure),
            systemWidth = 1200f,
            startX = 20f,
            startY = 50f,
            systemSpacing = 40f
        )

        val row = layout.rows.first()
        val firstMeasure = row.first()
        val firstStaves = firstMeasure.staves.sortedBy { it.staffNumber }
        assertEquals(2, firstStaves.size)

        val upper = firstStaves.first { it.staffNumber == 1 }
        val lower = firstStaves.first { it.staffNumber == 2 }
        val gap = lower.stave.getTopLineTopY() - upper.stave.getBottomLineBottomY()
        assertTrue(gap >= 2f * maxOf(upper.stave.spacingBetweenLines, lower.stave.spacingBetweenLines))
    }

    private fun buildMeasure(noteCount: Int): MusicSheetToVF.RenderedMeasure {
        val voice = VFVoice("4/4")
        val notes = List(noteCount.coerceAtLeast(1)) {
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf("c/4"),
                    duration = "4",
                    glyphFontScale = 40f
                )
            )
        }
        voice.addTickables(notes)

        val stave = VFStave(
            x = 0f,
            y = 60f,
            width = 500f
        )

        return MusicSheetToVF.RenderedMeasure(
            measureNumber = 1,
            staves = listOf(
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 1,
                    resolvedClefType = "treble",
                    stave = stave,
                    voices = listOf(voice),
                    beams = emptyList(),
                    ties = emptyList()
                )
            )
        )
    }

    private fun buildGrandStaffMeasureForOverlap(): MusicSheetToVF.RenderedMeasure {
        val upperVoice = VFVoice("4/4")
        upperVoice.addTickable(
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf("c/2"),
                    duration = "4",
                    glyphFontScale = 40f
                )
            )
        )

        val lowerVoice = VFVoice("4/4")
        lowerVoice.addTickable(
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf("c/6"),
                    duration = "4",
                    glyphFontScale = 40f
                )
            )
        )

        val upperStave = VFStave(x = 0f, y = 60f, width = 500f)
        val lowerStave = VFStave(x = 0f, y = 148f, width = 500f)

        return MusicSheetToVF.RenderedMeasure(
            measureNumber = 1,
            staves = listOf(
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 1,
                    resolvedClefType = "treble",
                    stave = upperStave,
                    voices = listOf(upperVoice),
                    beams = emptyList(),
                    ties = emptyList()
                ),
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 2,
                    resolvedClefType = "bass",
                    stave = lowerStave,
                    voices = listOf(lowerVoice),
                    beams = emptyList(),
                    ties = emptyList()
                )
            )
        )
    }
}
