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
        drawSpan(ctx, sv.getTopLineTopY(), sv.getBottomLineBottomY(), x)
    }

    fun drawSpan(ctx: VexRenderingContext, topY: Float, bottomY: Float, anchorX: Float = x) {
        when (type) {
            VFBarlineType.NONE -> return
            VFBarlineType.SINGLE -> drawThin(ctx, anchorX, topY, bottomY)
            VFBarlineType.DOUBLE -> {
                drawThin(ctx, anchorX - thinWidth - 2f, topY, bottomY)
                drawThin(ctx, anchorX, topY, bottomY)
            }
            VFBarlineType.END -> {
                drawThin(ctx, anchorX - thickWidth - 2f, topY, bottomY)
                drawThick(ctx, anchorX, topY, bottomY)
            }
            VFBarlineType.REPEAT_BEGIN -> {
                drawThick(ctx, anchorX, topY, bottomY)
                drawThin(ctx, anchorX + thickWidth + 2f, topY, bottomY)
                drawDots(ctx, anchorX + thickWidth + 2f + dotPad, topY, bottomY, rightSide = true)
            }
            VFBarlineType.REPEAT_END -> {
                drawDots(ctx, anchorX - dotPad, topY, bottomY, rightSide = false)
                drawThin(ctx, anchorX - dotPad - dotRadius * 3f, topY, bottomY)
                drawThick(ctx, anchorX, topY, bottomY)
            }
            VFBarlineType.REPEAT_BOTH -> {
                drawDots(ctx, anchorX - dotPad, topY, bottomY, rightSide = false)
                drawThin(ctx, anchorX - dotPad - dotRadius * 3f, topY, bottomY)
                drawThick(ctx, anchorX, topY, bottomY)
                drawThin(ctx, anchorX + thickWidth + 2f, topY, bottomY)
                drawDots(ctx, anchorX + thickWidth + 2f + dotPad, topY, bottomY, rightSide = true)
            }
        }
    }

    private fun drawThin(ctx: VexRenderingContext, atX: Float, topY: Float, bottomY: Float) {
        ctx.lineWidth = thinWidth
        ctx.beginPath()
        ctx.moveTo(atX, topY)
        ctx.lineTo(atX, bottomY)
        ctx.stroke()
    }

    private fun drawThick(ctx: VexRenderingContext, atX: Float, topY: Float, bottomY: Float) {
        ctx.fillRect(
            atX - thickWidth,
            topY,
            thickWidth,
            bottomY - topY
        )
    }

    private fun drawDots(
        ctx: VexRenderingContext,
        atX: Float,
        topY: Float,
        bottomY: Float,
        rightSide: Boolean
    ) {
        val height = bottomY - topY
        val topDotY = topY + height * 0.4f
        val botDotY = topY + height * 0.6f
        val cx = if (rightSide) atX + dotRadius else atX - dotRadius
        for (dotY in listOf(topDotY, botDotY)) {
            ctx.beginPath()
            ctx.arc(cx, dotY, dotRadius, 0f, (2f * PI).toFloat(), false)
            ctx.fill()
        }
    }
}
