package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables

/**
 * Renders a multi-measure rest: a thick horizontal bar spanning ~60% of the measure,
 * with vertical serif lines at each end and the measure count above the staff.
 */
class VFMultiMeasureRest(
    val count: Int,
    val staffLineSpacingPx: Float
) {
    private data class MmrLayout(
        val centerX: Float,
        val barLeft: Float,
        val barTop: Float,
        val barWidth: Float,
        val barHeight: Float,
        val serifTop: Float,
        val serifHeight: Float,
        val numberBottomY: Float,
        val staffSpacing: Float,
        val glyphSizePx: Float
    )

    companion object {
        private const val SMUFL_EM_IN_STAFF_SPACES = 4f
        private const val BAR_WIDTH_FRACTION = 0.6f
        private const val BAR_THICKNESS_SPACES = 0.8f
        private const val COUNT_GAP_ABOVE_STAFF_SPACES = 0.5f
    }

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        val layout = computeLayout(stave)

        ctx.fillRect(layout.barLeft, layout.barTop, layout.barWidth, layout.barHeight)

        // Thin serif vertical lines at each end
        val serifThickness = layout.staffSpacing * 0.12f
        ctx.fillRect(
            layout.barLeft - serifThickness / 2f,
            layout.serifTop,
            serifThickness,
            layout.serifHeight
        )
        ctx.fillRect(
            layout.barLeft + layout.barWidth - serifThickness / 2f,
            layout.serifTop,
            serifThickness,
            layout.serifHeight
        )

        // Count number centered above the staff
        drawCountNumber(layout, ctx)
    }

    fun verticalBounds(stave: VFStave): Pair<Float, Float> {
        val layout = computeLayout(stave)
        var top = minOf(layout.barTop, layout.serifTop)
        var bottom = maxOf(layout.barTop + layout.barHeight, layout.serifTop + layout.serifHeight)

        val digits = count.toString().mapNotNull { digitGlyph(it) }
        if (digits.isEmpty()) return top to bottom

        for (digit in digits) {
            val name = digitGlyphName(digit)
            val raw = name?.let { safeGlyphBounds(it) }
            if (raw != null) {
                val scaled = raw.scaled(layout.staffSpacing)
                val originY = digitOriginYAlignedToBottom(digit, layout.numberBottomY, layout.staffSpacing)
                val glyphTop = originY - scaled.northeast.y
                val glyphBottom = originY - scaled.southwest.y
                top = minOf(top, glyphTop)
                bottom = maxOf(bottom, glyphBottom)
            } else {
                val fallbackTop = layout.numberBottomY - layout.staffSpacing * 1.5f
                val fallbackBottom = layout.numberBottomY
                top = minOf(top, fallbackTop)
                bottom = maxOf(bottom, fallbackBottom)
            }
        }

        return top to bottom
    }

    private fun computeLayout(stave: VFStave): MmrLayout {
        val s = stave.spacingBetweenLines
        val glyphSizePx = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES

        val noteStartX = runCatching { stave.getNoteStartX() }
            .getOrElse { stave.x + s * 2f }
        val noteEndX = stave.x + stave.width - s * 0.5f
        val centerX = (noteStartX + noteEndX) / 2f
        val availableWidth = noteEndX - noteStartX

        val barCenterY = stave.getYForMusicLine(3)
        val barHeight = s * BAR_THICKNESS_SPACES
        val barTop = barCenterY - (barHeight / 2f)
        val barWidth = availableWidth * BAR_WIDTH_FRACTION
        val barLeft = centerX - barWidth / 2f

        val serifTop = stave.getYForMusicLine(4)
        val serifBottom = stave.getYForMusicLine(2)
        val serifHeight = (serifBottom - serifTop).coerceAtLeast(0f)
        val numberBottomY = stave.getYForLine(0f) - s * COUNT_GAP_ABOVE_STAFF_SPACES

        return MmrLayout(
            centerX = centerX,
            barLeft = barLeft,
            barTop = barTop,
            barWidth = barWidth,
            barHeight = barHeight,
            serifTop = serifTop,
            serifHeight = serifHeight,
            numberBottomY = numberBottomY,
            staffSpacing = s,
            glyphSizePx = glyphSizePx
        )
    }

    private fun drawCountNumber(layout: MmrLayout, ctx: VexRenderingContext) {
        val digits = count.toString().mapNotNull { digitGlyph(it) }
        if (digits.isEmpty()) return

        val digitWidths = digits.map { digitAdvance(it, layout.staffSpacing) }
        val totalWidth = digitWidths.sum()

        var curX = layout.centerX - totalWidth / 2f
        for ((i, digit) in digits.withIndex()) {
            val bearing = digitLeftBearing(digit, layout.staffSpacing)
            val originY = digitOriginYAlignedToBottom(digit, layout.numberBottomY, layout.staffSpacing)
            ctx.drawSmuflGlyph(digit, curX - bearing, originY, layout.glyphSizePx)
            curX += digitWidths[i]
        }
    }

    private fun digitGlyph(c: Char): Int? = when (c) {
        '0' -> VFTables.GLYPH_TIME_SIG_0
        '1' -> VFTables.GLYPH_TIME_SIG_1
        '2' -> VFTables.GLYPH_TIME_SIG_2
        '3' -> VFTables.GLYPH_TIME_SIG_3
        '4' -> VFTables.GLYPH_TIME_SIG_4
        '5' -> VFTables.GLYPH_TIME_SIG_5
        '6' -> VFTables.GLYPH_TIME_SIG_6
        '7' -> VFTables.GLYPH_TIME_SIG_7
        '8' -> VFTables.GLYPH_TIME_SIG_8
        '9' -> VFTables.GLYPH_TIME_SIG_9
        else -> null
    }

    private fun digitGlyphName(codepoint: Int): String? = when (codepoint) {
        VFTables.GLYPH_TIME_SIG_0 -> "timeSig0"
        VFTables.GLYPH_TIME_SIG_1 -> "timeSig1"
        VFTables.GLYPH_TIME_SIG_2 -> "timeSig2"
        VFTables.GLYPH_TIME_SIG_3 -> "timeSig3"
        VFTables.GLYPH_TIME_SIG_4 -> "timeSig4"
        VFTables.GLYPH_TIME_SIG_5 -> "timeSig5"
        VFTables.GLYPH_TIME_SIG_6 -> "timeSig6"
        VFTables.GLYPH_TIME_SIG_7 -> "timeSig7"
        VFTables.GLYPH_TIME_SIG_8 -> "timeSig8"
        VFTables.GLYPH_TIME_SIG_9 -> "timeSig9"
        else -> null
    }

    private fun digitAdvance(codepoint: Int, staffSpacing: Float): Float =
        digitGlyphName(codepoint)
            ?.let { safeGlyphBounds(it)?.scaled(staffSpacing)?.width }
            ?: (staffSpacing * 0.6f)

    private fun digitLeftBearing(codepoint: Int, staffSpacing: Float): Float =
        digitGlyphName(codepoint)
            ?.let { safeGlyphBounds(it)?.scaled(staffSpacing)?.southwest?.x }
            ?: 0f

    private fun digitOriginYAlignedToBottom(codepoint: Int, targetBottomY: Float, staffSpacing: Float): Float {
        val name = digitGlyphName(codepoint) ?: return targetBottomY
        val raw = safeGlyphBounds(name) ?: return targetBottomY
        val scaled = raw.scaled(staffSpacing)
        return targetBottomY + scaled.southwest.y
    }

    private fun safeGlyphBounds(glyphName: String) =
        runCatching { VFGlyphBoundingBoxManager.get(glyphName) }.getOrNull()
}
