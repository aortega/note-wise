package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFStaveNote
import kotlin.math.abs

data class VFTieNotes(
    val firstNote: VFStaveNote? = null,
    val lastNote: VFStaveNote? = null,
    val firstIndexes: List<Int> = listOf(0),
    val lastIndexes: List<Int> = listOf(0)
)

/**
 * Curved tie connecting two same-pitch notes.
 */
class VFTie(private var notes: VFTieNotes) {

    data class RenderOptions(
        val cp1: Float = 8f,
        val cp2: Float = 12f,
        val shortTieCutoff: Float = 10f,
        val cp1Short: Float = 2f,
        val cp2Short: Float = 8f,
        val firstXShift: Float = 0f,
        val lastXShift: Float = 0f,
        val tieSpacing: Float = 0f,
        val yShift: Float = 7f
    )

    var renderOptions = RenderOptions()

    fun setNotes(n: VFTieNotes) {
        notes = n
    }

    fun getNotes(): VFTieNotes = notes

    fun isPartial(): Boolean = notes.firstNote == null || notes.lastNote == null

    fun draw(ctx: VexRenderingContext) {
        val (firstNote, lastNote, firstIdxs, lastIdxs) = notes
        val count = minOf(firstIdxs.size, lastIdxs.size)

        val firstX = firstNote?.getTieRightX()
            ?: firstNote?.getStave()?.getTieStartX()
            ?: lastNote?.getStave()?.getTieStartX()
            ?: return
        val lastX = lastNote?.getTieLeftX()
            ?: lastNote?.getStave()?.getTieEndX()
            ?: firstNote?.getStave()?.getTieEndX()
            ?: return

        for (i in 0 until count) {
            val firstY = firstNote?.getYs()?.getOrNull(firstIdxs[i])
                ?: lastNote?.getYs()?.getOrNull(lastIdxs[i])
                ?: continue
            val lastY = lastNote?.getYs()?.getOrNull(lastIdxs[i])
                ?: firstNote?.getYs()?.getOrNull(firstIdxs[i])
                ?: continue
            val stemDir = firstNote?.getStemDirection() ?: lastNote?.getStemDirection() ?: 1
            // Draw ties opposite to stem side in screen coordinates:
            // stems up (1) -> tie below notes (+y), stems down (-1) -> tie above notes (-y).
            val tieDir = stemDir

            renderTie(
                ctx,
                firstX + renderOptions.firstXShift,
                lastX + renderOptions.lastXShift,
                firstY + renderOptions.tieSpacing,
                lastY + renderOptions.tieSpacing,
                tieDir
            )
        }
    }

    private fun renderTie(
        ctx: VexRenderingContext,
        x1: Float,
        x2: Float,
        y1: Float,
        y2: Float,
        direction: Int
    ) {
        val yDir = direction.toFloat()
        val span = abs(x2 - x1)
        val y1Shifted = y1 + yDir * renderOptions.yShift
        val y2Shifted = y2 + yDir * renderOptions.yShift
        val avgY = (y1Shifted + y2Shifted) * 0.5f
        val controlX = (x1 + x2) * 0.5f

        val staveSpacing = notes.firstNote?.getStave()?.spacingBetweenLines
            ?: notes.lastNote?.getStave()?.spacingBetweenLines
            ?: 10f

        // Scale tie height by span, clamped by stave spacing to avoid stretched/flat ties.
        val arcDepth = (span * 0.12f).coerceIn(staveSpacing * 0.6f, staveSpacing * 1.8f)
        // Keep a stable visual lens thickness proportional to stave spacing.
        val tieThickness = (staveSpacing * 0.22f).coerceIn(1.2f, 4.0f)

        val topControlY = avgY + yDir * arcDepth
        val bottomControlY = avgY + yDir * (arcDepth + tieThickness)

        ctx.beginPath()
        ctx.moveTo(x1, y1Shifted)
        ctx.quadraticCurveTo(controlX, topControlY, x2, y2Shifted)
        ctx.quadraticCurveTo(controlX, bottomControlY, x1, y1Shifted)
        ctx.closePath()
        ctx.fill()
    }
}
