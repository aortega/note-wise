package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.parser.MusicSheetToVF
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFSystemTest {

    @Test
    fun `grand staff spans internal and outer system barlines`() {
        val firstTop = VFStave(20f, 50f, 100f).apply {
            startBarline = VFBarline(VFBarlineType.SINGLE)
            endBarline = VFBarline(VFBarlineType.SINGLE)
        }
        val firstBottom = VFStave(20f, 140f, 100f).apply {
            startBarline = VFBarline(VFBarlineType.SINGLE)
            endBarline = VFBarline(VFBarlineType.SINGLE)
        }
        val secondTop = VFStave(120f, 50f, 100f).apply {
            startBarline = VFBarline(VFBarlineType.NONE)
            endBarline = VFBarline(VFBarlineType.SINGLE)
        }
        val secondBottom = VFStave(120f, 140f, 100f).apply {
            startBarline = VFBarline(VFBarlineType.NONE)
            endBarline = VFBarline(VFBarlineType.SINGLE)
        }

        val system = VFSystem(x = 20f, y = 50f, width = 200f).apply {
            addMeasure(renderedMeasure(1, firstTop, firstBottom))
            addMeasure(renderedMeasure(2, secondTop, secondBottom))
        }
        val ctx = PathRecordingContext()

        system.draw(ctx)

        val topY = firstTop.getTopLineTopY()
        val bottomY = firstBottom.getBottomLineBottomY()
        val leftBoundaryX = firstTop.x
        val internalBoundaryX = firstTop.x + firstTop.width
        val rightBoundaryX = secondTop.x + secondTop.width

        assertTrue(ctx.hasVerticalSpan(leftBoundaryX, topY, bottomY))
        assertTrue(ctx.hasVerticalSpan(internalBoundaryX, topY, bottomY))
        assertTrue(ctx.hasVerticalSpan(rightBoundaryX, topY, bottomY))
    }

    private fun renderedMeasure(
        measureNumber: Int,
        top: VFStave,
        bottom: VFStave
    ): MusicSheetToVF.RenderedMeasure {
        return MusicSheetToVF.RenderedMeasure(
            measureNumber = measureNumber,
            staves = listOf(
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 1,
                    resolvedClefType = "treble",
                    stave = top,
                    voices = emptyList(),
                    beams = emptyList(),
                    ties = emptyList()
                ),
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 2,
                    resolvedClefType = "bass",
                    stave = bottom,
                    voices = emptyList(),
                    beams = emptyList(),
                    ties = emptyList()
                )
            )
        )
    }

    private class PathRecordingContext : VexRenderingContext() {
        data class Segment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

        private var startX: Float? = null
        private var startY: Float? = null
        private var endX: Float? = null
        private var endY: Float? = null

        val strokeSegments = mutableListOf<Segment>()

        override fun beginPath() {
            super.beginPath()
            startX = null
            startY = null
            endX = null
            endY = null
        }

        override fun moveTo(x: Float, y: Float) {
            super.moveTo(x, y)
            startX = x
            startY = y
            endX = x
            endY = y
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            endX = x
            endY = y
        }

        override fun stroke() {
            val x1 = startX
            val y1 = startY
            val x2 = endX
            val y2 = endY
            if (x1 != null && y1 != null && x2 != null && y2 != null) {
                strokeSegments += Segment(x1, y1, x2, y2)
            }
            super.stroke()
        }

        override fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
            // Not needed for the barline span regression.
        }

        fun hasVerticalSpan(x: Float, topY: Float, bottomY: Float): Boolean {
            return strokeSegments.any { segment ->
                abs(segment.x1 - segment.x2) <= 0.01f &&
                    abs(segment.x1 - x) <= 0.01f &&
                    abs(minOf(segment.y1, segment.y2) - topY) <= 0.01f &&
                    abs(maxOf(segment.y1, segment.y2) - bottomY) <= 0.01f
            }
        }
    }
}