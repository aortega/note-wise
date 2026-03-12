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
    fun `default inter row spacing stays content driven`() {
        val measures = List(12) { buildMeasure(noteCount = 4) }

        val layout = VFLineBreaker.layout(
            measures = measures,
            systemWidth = 800f,
            startX = 20f,
            startY = 60f
        )

        assertTrue("Expected at least two rows", layout.rows.size >= 2)

        val firstRowBottom = layout.systemY[0] + layout.systemHeights[0]
        val secondRowTop = layout.systemY[1]
        val gap = secondRowTop - firstRowBottom

        assertTrue("Gap should be compact, was $gap", gap <= 24f)
        assertTrue("Gap should still be positive, was $gap", gap >= 18f)
    }

    @Test
    fun `consecutive staves keep minimum 6x spacing between stave boundaries`() {
        val measure = buildGrandStaffMeasure(
            upperKey = "c/5",
            lowerKey = "c/3"
        )

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
        val gap = lower.stave.getTopLineTopY() - upper.stave.getBottomLineBottomY()

        assertTrue(gap >= 6f * upper.stave.spacingBetweenLines)
    }

    @Test
    fun `moderate inward ledger content keeps total grand staff gap at 6 spaces`() {
        val measure = buildGrandStaffMeasure(
            upperKey = "e/3",
            lowerKey = "c/3"
        )

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
        val gap = lower.stave.getTopLineTopY() - upper.stave.getBottomLineBottomY()

        assertEquals(6f * upper.stave.spacingBetweenLines, gap, 1.0f)
    }

    @Test
    fun `grand staff rows reserve left inset for brace`() {
        val measure = buildGrandStaffMeasure(
            upperKey = "c/5",
            lowerKey = "c/3"
        )
        val startX = 20f
        val systemWidth = 600f

        val layout = VFLineBreaker.layout(
            measures = listOf(measure),
            systemWidth = systemWidth,
            startX = startX,
            startY = 50f,
            systemSpacing = 40f
        )

        val row = layout.rows.first()
        val firstMeasure = row.first()
        val orderedStaves = firstMeasure.staves.sortedBy { it.staffNumber }
        val firstStave = orderedStaves.first().stave
        val lastStave = orderedStaves.last().stave
        val expectedInset = VFSystem.grandStaffBraceReservedInsetPx(firstStave, lastStave)
        val rowRight = row.maxOf { rendered ->
            rendered.staves.maxOf { it.stave.x + it.stave.width }
        }

        assertEquals(startX + expectedInset, firstStave.x, 0.01f)
        assertTrue(rowRight <= startX + systemWidth + 0.01f)
    }

    @Test
    fun `consecutive staves cap expansion at 19x spacing when ledger lines intrude`() {
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

        assertTrue(gap >= 6f * upper.stave.spacingBetweenLines)
        assertTrue(gap <= 19f * maxOf(upper.stave.spacingBetweenLines, lower.stave.spacingBetweenLines) + 0.01f)
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
        assertTrue(gap >= 6f * maxOf(upper.stave.spacingBetweenLines, lower.stave.spacingBetweenLines))
    }

    @Test
    fun `multi measure rest count contributes to row content bounds`() {
        val first = buildMeasure(noteCount = 4).copy(measureNumber = 1)
        val secondWithoutMmr = buildEmptyMeasure(measureNumber = 2, includeMultiMeasureRest = false)
        val secondWithMmr = buildEmptyMeasure(measureNumber = 2, includeMultiMeasureRest = true)

        fun secondRowHeight(second: MusicSheetToVF.RenderedMeasure): Float {
            val layout = VFLineBreaker.layout(
                measures = listOf(first, second),
                systemWidth = 150f,
                startX = 20f,
                startY = 60f,
                systemSpacing = 0f
            )
            assertTrue("Expected wrapping into two rows", layout.rows.size >= 2)
            return layout.systemHeights[1]
        }

        val withoutMmrHeight = secondRowHeight(secondWithoutMmr)
        val withMmrHeight = secondRowHeight(secondWithMmr)

        assertTrue(
            "MMR count should expand row content bounds (without=$withoutMmrHeight, with=$withMmrHeight)",
            withMmrHeight > withoutMmrHeight + 0.01f
        )
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
        return buildGrandStaffMeasure(upperKey = "c/2", lowerKey = "c/6")
    }

    private fun buildGrandStaffMeasure(
        upperKey: String,
        lowerKey: String
    ): MusicSheetToVF.RenderedMeasure {
        val upperVoice = VFVoice("4/4")
        upperVoice.addTickable(
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf(upperKey),
                    duration = "4",
                    glyphFontScale = 40f
                )
            )
        )

        val lowerVoice = VFVoice("4/4")
        lowerVoice.addTickable(
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf(lowerKey),
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

    private fun buildEmptyMeasure(
        measureNumber: Int,
        includeMultiMeasureRest: Boolean
    ): MusicSheetToVF.RenderedMeasure {
        val stave = VFStave(
            x = 0f,
            y = 60f,
            width = 500f
        )

        return MusicSheetToVF.RenderedMeasure(
            measureNumber = measureNumber,
            staves = listOf(
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 1,
                    resolvedClefType = "treble",
                    stave = stave,
                    voices = emptyList(),
                    beams = emptyList(),
                    ties = emptyList(),
                    multiMeasureRest = if (includeMultiMeasureRest) {
                        VFMultiMeasureRest(count = 2, staffLineSpacingPx = stave.spacingBetweenLines)
                    } else {
                        null
                    }
                )
            )
        )
    }
}
