package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import kotlin.math.abs

/**
 * Connects eighth-or-shorter notes with one or more filled beams.
 */
class VFBeam(notes: List<VFStaveNote>) {

    private val notes: List<VFStaveNote> = notes.toList()

    var beamThickness: Float = VFMetrics.BEAM_THICKNESS
    var beamSpacing: Float = VFMetrics.BEAM_SPACING
    var maxBeamSlope: Float = 0.4f

    init {
        this.notes.forEach { it.setBeamed(true) }
    }

    fun draw(ctx: VexRenderingContext, stave: VFStave) {
        if (notes.size < 2) return

        val orderedNotes = notes.sortedBy { it.x }

        beamThickness = stave.options.engravingOptions.beamThickness
        beamSpacing = stave.options.engravingOptions.beamSpacing
        maxBeamSlope = stave.options.engravingOptions.maxBeamSlope

        val direction = computeDirection(orderedNotes)
        val stemXs = orderedNotes.map { stemX(it, direction) }
        val stemTipYs = computeStemTipYs(orderedNotes, stave, direction)
        val slope = computeSlope(stemXs, stemTipYs)
        val adjustedTips = adjustStemTips(stemXs, stemTipYs, slope)

        drawStems(ctx, orderedNotes, stemXs, adjustedTips, direction)
        drawPrimaryBeam(ctx, stemXs, adjustedTips, direction)
        drawSecondaryBeams(ctx, orderedNotes, stemXs, adjustedTips, direction)
    }

    private fun computeDirection(orderedNotes: List<VFStaveNote>): Int {
        val upCount = orderedNotes.count { it.getStemDirection() == VFStaveNoteStruct.STEM_UP }
        return if (upCount * 2 >= notes.size) {
            VFStaveNoteStruct.STEM_UP
        } else {
            VFStaveNoteStruct.STEM_DOWN
        }
    }

    private fun stemX(note: VFStaveNote, direction: Int): Float {
        return note.getStemX(direction)
    }

    private fun computeStemTipYs(
        orderedNotes: List<VFStaveNote>,
        stave: VFStave,
        direction: Int
    ): List<Float> {
        val stemHeightPx = VFMetrics.STEM_HEIGHT_SPACES * stave.spacingBetweenLines
        return orderedNotes.map { note ->
            val noteHeadY = note.getYs().firstOrNull() ?: stave.getYForLine(2f)
            noteHeadY - direction * stemHeightPx
        }
    }

    private fun computeSlope(xs: List<Float>, ys: List<Float>): Float {
        val dx = xs.last() - xs.first()
        if (dx == 0f) return 0f
        val rawSlope = (ys.last() - ys.first()) / dx
        return rawSlope.coerceIn(-maxBeamSlope, maxBeamSlope)
    }

    private fun adjustStemTips(xs: List<Float>, idealTips: List<Float>, slope: Float): List<Float> {
        val firstX = xs.first()
        val firstY = idealTips.first()
        return xs.map { x -> firstY + slope * (x - firstX) }
    }

    private fun drawStems(
        ctx: VexRenderingContext,
        orderedNotes: List<VFStaveNote>,
        xs: List<Float>,
        tips: List<Float>,
        direction: Int
    ) {
        val stemStrokeWidth = 1.5f
        // Keep stem caps visually inside the beam by ending half a stroke into the beam body.
        val joinInset = maxOf(0.5f, stemStrokeWidth * 0.5f)
        ctx.lineWidth = stemStrokeWidth
        for (i in orderedNotes.indices) {
            val noteHeadY = orderedNotes[i].getYs().firstOrNull() ?: continue
            ctx.beginPath()
            ctx.moveTo(xs[i], noteHeadY)
            ctx.lineTo(xs[i], tips[i] + direction * joinInset)
            ctx.stroke()
        }
    }

    private fun drawPrimaryBeam(
        ctx: VexRenderingContext,
        xs: List<Float>,
        tips: List<Float>,
        direction: Int
    ) {
        val dir = direction.toFloat()
        val x1 = xs.first()
        val y1 = tips.first()
        val x2 = xs.last()
        val y2 = tips.last()

        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.lineTo(x2, y2)
        ctx.lineTo(x2, y2 + dir * beamThickness)
        ctx.lineTo(x1, y1 + dir * beamThickness)
        ctx.closePath()
        ctx.fill()
    }

    private fun drawSecondaryBeams(
        ctx: VexRenderingContext,
        orderedNotes: List<VFStaveNote>,
        xs: List<Float>,
        tips: List<Float>,
        direction: Int
    ) {
        fun beamCount(note: VFStaveNote): Int = when {
            note.duration <= VFFraction.of(1, 32) -> 3
            note.duration <= VFFraction.of(1, 16) -> 2
            else -> 1
        }

        val maxBeams = orderedNotes.maxOf { beamCount(it) }
        val dir = direction.toFloat()

        for (level in 2..maxBeams) {
            val offset = dir * (beamThickness + beamSpacing) * (level - 1)
            var runStart = -1

            for (i in orderedNotes.indices) {
                val needs = beamCount(orderedNotes[i]) >= level
                if (needs && runStart < 0) runStart = i

                if ((!needs || i == orderedNotes.lastIndex) && runStart >= 0) {
                    val runEnd = if (needs) i else i - 1
                    val x1 = xs[runStart]
                    val y1 = tips[runStart] + offset
                    val x2 = xs[runEnd]
                    val y2 = tips[runEnd] + offset

                    ctx.beginPath()
                    ctx.moveTo(x1, y1)
                    ctx.lineTo(x2, y2)
                    ctx.lineTo(x2, y2 + dir * beamThickness)
                    ctx.lineTo(x1, y1 + dir * beamThickness)
                    ctx.closePath()
                    ctx.fill()

                    runStart = -1
                }
            }
        }
    }
}
