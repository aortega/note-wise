package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFStaveNote
import kotlin.math.log

data class VFSlurOptions(
    val thickness: Float = 2f,
    val xShift: Float = 0f,
    val yShift: Float = 10f,
    val invert: Boolean = false
)

/**
 * Phrase slur connecting notes with a stroked bezier arc.
 */
class VFSlur(
    private var fromNote: VFStaveNote? = null,
    private var toNote: VFStaveNote? = null,
    val options: VFSlurOptions = VFSlurOptions()
) {
    fun setNotes(from: VFStaveNote?, to: VFStaveNote?) {
        fromNote = from
        toNote = to
    }

    fun isPartial(): Boolean = fromNote == null || toNote == null

    fun draw(ctx: VexRenderingContext) {
        val from = fromNote ?: return
        val to = toNote ?: return
        val fromY = from.getYs().firstOrNull() ?: return
        val toY = to.getYs().firstOrNull() ?: return

        val stemDir = from.getStemDirection()
        val direction = if (options.invert) -stemDir else stemDir
        val slurDir = -direction.toFloat()

        val x1 = from.getTieRightX() + options.xShift
        val x2 = to.getTieLeftX()
        val y1 = fromY + slurDir * options.yShift
        val y2 = toY + slurDir * options.yShift

        val span = x2 - x1
        val logPart = log((span / 50f).toDouble().coerceAtLeast(1.0), 2.0).toFloat()
        val height = minOf(40f, options.yShift + logPart * 10f)

        val cpX1 = x1 + span * 0.30f
        val cpX2 = x2 - span * 0.30f
        val cpY1 = y1 + slurDir * height
        val cpY2 = y2 + slurDir * height

        ctx.lineWidth = options.thickness
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.bezierCurveTo(cpX1, cpY1, cpX2, cpY2, x2, y2)
        ctx.stroke()
    }
}
