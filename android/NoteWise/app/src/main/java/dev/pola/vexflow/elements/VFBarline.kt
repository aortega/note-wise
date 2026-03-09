package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import kotlin.math.PI

enum class VFBarlineType {
    SINGLE,
    DOUBLE,
    END,
    REPEAT_BEGIN,
    REPEAT_END,
    REPEAT_BOTH,
    NONE;

    companion object {
        fun fromString(s: String): VFBarlineType = when (s.lowercase()) {
            "single" -> SINGLE
            "double" -> DOUBLE
            "end" -> END
            "repeat-begin", "repeatbegin" -> REPEAT_BEGIN
            "repeat-end", "repeatend" -> REPEAT_END
            "repeat-both", "repeatboth" -> REPEAT_BOTH
            "none" -> NONE
            else -> SINGLE
        }
    }
}

/**
 * Draws a barline at position x within stave.
 */
class VFBarline(var type: VFBarlineType = VFBarlineType.SINGLE) {
    var stave: VFStave? = null
    var x: Float = 0f

    private val thinWidth = 1.5f
    private val thickWidth = 5f
    private val dotRadius = 1.5f
    private val dotPad = 4f

    /**
     * Returns how far to the LEFT of the barline anchor (stave.x + stave.width) the barline's
     * leftmost visual pixel sits. Used by the formatter and estimator to reserve a right-side
     * safety margin so note glyphs never overlap the barline strokes.
     */
    fun leftExtentPx(): Float = when (type) {
        VFBarlineType.NONE   -> 0f
        VFBarlineType.SINGLE -> thinWidth / 2f                            // 0.75 px
        VFBarlineType.DOUBLE -> thinWidth / 2f + 2f + thinWidth           // ~5.25 px
        VFBarlineType.END    -> thickWidth + 2f + thinWidth / 2f          // 7.75 px
        VFBarlineType.REPEAT_BEGIN -> thinWidth / 2f                      // dots to the right, thin is leftmost
        VFBarlineType.REPEAT_END   -> dotPad + dotRadius * 3f             // dot column is leftmost
        VFBarlineType.REPEAT_BOTH  -> dotPad + dotRadius * 3f
    }

    fun draw(ctx: VexRenderingContext) {
        val sv = stave ?: return
        when (type) {
            VFBarlineType.NONE -> return
            VFBarlineType.SINGLE -> drawThin(sv, ctx, x)
            VFBarlineType.DOUBLE -> {
                drawThin(sv, ctx, x - thinWidth - 2f)
                drawThin(sv, ctx, x)
            }
            VFBarlineType.END -> {
                drawThin(sv, ctx, x - thickWidth - 2f)
                drawThick(sv, ctx, x)
            }
            VFBarlineType.REPEAT_BEGIN -> {
                drawThick(sv, ctx, x)
                drawThin(sv, ctx, x + thickWidth + 2f)
                drawDots(sv, ctx, x + thickWidth + 2f + dotPad, rightSide = true)
            }
            VFBarlineType.REPEAT_END -> {
                drawDots(sv, ctx, x - dotPad, rightSide = false)
                drawThin(sv, ctx, x - dotPad - dotRadius * 3f)
                drawThick(sv, ctx, x)
            }
            VFBarlineType.REPEAT_BOTH -> {
                drawDots(sv, ctx, x - dotPad, rightSide = false)
                drawThin(sv, ctx, x - dotPad - dotRadius * 3f)
                drawThick(sv, ctx, x)
                drawThin(sv, ctx, x + thickWidth + 2f)
                drawDots(sv, ctx, x + thickWidth + 2f + dotPad, rightSide = true)
            }
        }
    }

    private fun drawThin(sv: VFStave, ctx: VexRenderingContext, atX: Float) {
        ctx.lineWidth = thinWidth
        ctx.beginPath()
        ctx.moveTo(atX, sv.getTopLineTopY())
        ctx.lineTo(atX, sv.getBottomLineBottomY())
        ctx.stroke()
    }

    private fun drawThick(sv: VFStave, ctx: VexRenderingContext, atX: Float) {
        ctx.fillRect(
            atX - thickWidth,
            sv.getTopLineTopY(),
            thickWidth,
            sv.getBottomLineBottomY() - sv.getTopLineTopY()
        )
    }

    private fun drawDots(sv: VFStave, ctx: VexRenderingContext, atX: Float, rightSide: Boolean) {
        val topDotY = sv.getYForLine(1.5f)
        val botDotY = sv.getYForLine(2.5f)
        val cx = if (rightSide) atX + dotRadius else atX - dotRadius
        for (dotY in listOf(topDotY, botDotY)) {
            ctx.beginPath()
            ctx.arc(cx, dotY, dotRadius, 0f, (2f * PI).toFloat(), false)
            ctx.fill()
        }
    }
}
