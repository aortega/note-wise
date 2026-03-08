package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFEngravingOptions
import dev.pola.vexflow.model.VFMetrics

data class VFStaveOptions(
    val numLines: Int = 5,
    val spacingBetweenLinesPx: Float = VFMetrics.DEFAULT_LINE_SPACING,
    val openingBoundarySpacingSpaces: Float = 1f,
    val engravingOptions: VFEngravingOptions = VFEngravingOptions()
)

/**
 * A single staff (set of horizontal lines) on the canvas.
 */
class VFStave(
    val x: Float,
    val y: Float,
    val width: Float,
    val options: VFStaveOptions = VFStaveOptions()
) {
    val numLines: Int = options.numLines
    val spacingBetweenLines: Float = options.spacingBetweenLinesPx

    var lineThickness: Float = 1f

    var clef: VFClef? = null
    var keySignature: VFKeySignature? = null
    var timeSignature: VFTimeSignature? = null

    var startBarline: VFBarline? = null
    var endBarline: VFBarline? = VFBarline(VFBarlineType.SINGLE)

    private var context: VexRenderingContext? = null

    fun setContext(ctx: VexRenderingContext) {
        context = ctx
    }

    fun getYForLine(line: Float): Float = y + line * spacingBetweenLines

    private fun openingBoundaryPaddingPx(): Float =
        spacingBetweenLines * options.openingBoundarySpacingSpaces

    private fun leftPaddingPx(): Float = openingBoundaryPaddingPx()

    /**
     * Converts a music-style line index (bottom line = 1) to top-based line index used by getYForLine.
     */
    fun musicLineToTopIndex(musicLine: Int): Float {
        val clamped = musicLine.coerceIn(1, numLines)
        return (numLines - clamped).toFloat()
    }

    /**
     * Converts a music-style line/space position (bottom line = 1, spaces use .5) to top-based index.
     */
    fun musicPositionToTopIndex(musicPosition: Float): Float {
        val minPos = 1f
        val maxPos = numLines.toFloat()
        val clamped = musicPosition.coerceIn(minPos, maxPos)
        return numLines.toFloat() - clamped
    }

    fun getYForMusicLine(musicLine: Int): Float = getYForLine(musicLineToTopIndex(musicLine))

    fun getYForMusicPosition(musicPosition: Float): Float =
        getYForLine(musicPositionToTopIndex(musicPosition))

    fun getYForNote(noteLine: Int): Float = y + noteLine * (spacingBetweenLines / 2f)

    fun getTopLineTopY(): Float = y - lineThickness / 2f

    fun getBottomLineBottomY(): Float = getYForLine((numLines - 1).toFloat()) + lineThickness / 2f

    fun getNoteStartX(): Float {
        var startX = x + leftPaddingPx()
        clef?.let {
            startX +=
                it.widthForStaffSpacing(spacingBetweenLines) +
                    openingBoundaryPaddingPx()
        }
        keySignature?.let {
            if (!it.isEmpty) {
                startX +=
                    it.widthForStaffSpacing(spacingBetweenLines) +
                        openingBoundaryPaddingPx()
            }
        }
        timeSignature?.let {
            startX +=
                it.widthForStaffSpacing(spacingBetweenLines) +
                    openingBoundaryPaddingPx()
        }
        return startX
    }

    fun getTieStartX(): Float = getNoteStartX()

    fun getTieEndX(): Float = x + width

    fun draw(ctx: VexRenderingContext) {
        drawLines(ctx)
        drawStartBarline(ctx)
        drawModifiers(ctx)
        drawEndBarline(ctx)
    }

    private fun drawLines(ctx: VexRenderingContext) {
        ctx.lineWidth = lineThickness
        ctx.strokeColor = android.graphics.Color.BLACK
        for (i in 0 until numLines) {
            val lineY = getYForLine(i.toFloat())
            ctx.beginPath()
            ctx.moveTo(x, lineY)
            ctx.lineTo(x + width, lineY)
            ctx.stroke()
        }
    }

    private fun drawStartBarline(ctx: VexRenderingContext) {
        val bl = startBarline ?: VFBarline(VFBarlineType.SINGLE)
        bl.stave = this
        bl.x = x
        bl.draw(ctx)
    }

    private fun drawEndBarline(ctx: VexRenderingContext) {
        val bl = endBarline ?: return
        bl.stave = this
        bl.x = x + width
        bl.draw(ctx)
    }

    private fun drawModifiers(ctx: VexRenderingContext) {
        var curX = x + leftPaddingPx()

        clef?.let {
            it.x = curX
            it.draw(this, ctx)
            curX +=
                it.widthForStaffSpacing(spacingBetweenLines) +
                    openingBoundaryPaddingPx()
        }
        keySignature?.let {
            if (!it.isEmpty) {
                it.x = curX
                it.draw(this, ctx)
                curX +=
                    it.widthForStaffSpacing(spacingBetweenLines) +
                        openingBoundaryPaddingPx()
            }
        }
        timeSignature?.let {
            it.x = curX
            it.draw(this, ctx)
        }
    }
}
