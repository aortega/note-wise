package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables

/**
 * Time signature element (M3).
 */
class VFTimeSignature(timeSpec: String = "4/4") {
    private val timeSpec: String = timeSpec

    var sizePx: Float = 40f
    var x: Float = 0f
    val width: Float
        get() = when {
            isCommonTime || isCutTime -> sizePx * 1.2f
            else -> maxOf(topGlyphs.size, bottomGlyphs.size) * (sizePx * 0.6f)
        }

    fun widthForStaffSpacing(staffSpacing: Float): Float {
        if (isCommonTime) {
            return VFGlyphBoundingBoxManager.get("timeSigCommon")?.scaled(staffSpacing)?.width ?: width
        }
        if (isCutTime) {
            return VFGlyphBoundingBoxManager.get("timeSigCutCommon")?.scaled(staffSpacing)?.width ?: width
        }

        fun numericRunWidth(glyphs: List<Int>): Float {
            if (glyphs.isEmpty()) return 0f
            var total = 0f
            for (glyph in glyphs) {
                total += digitAdvanceForStaffSpacing(glyph, staffSpacing)
            }
            return total
        }

        return maxOf(numericRunWidth(topGlyphs), numericRunWidth(bottomGlyphs))
    }

    private val isCommonTime: Boolean = timeSpec == "C"
    private val isCutTime: Boolean = timeSpec == "C|"
    private val topGlyphs: List<Int>
    private val bottomGlyphs: List<Int>

    init {
        if (isCommonTime || isCutTime) {
            topGlyphs = emptyList()
            bottomGlyphs = emptyList()
        } else {
            val parts = timeSpec.split('/')
            if (parts.size == 2) {
                topGlyphs = parts[0].mapNotNull { digitGlyph(it) }
                bottomGlyphs = parts[1].mapNotNull { digitGlyph(it) }
            } else {
                topGlyphs = emptyList()
                bottomGlyphs = emptyList()
            }
        }
    }

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        when {
            isCommonTime -> drawCommonGlyph(VFTables.GLYPH_TIME_SIG_COMMON, "timeSigCommon", stave, ctx)
            isCutTime -> drawCommonGlyph(VFTables.GLYPH_TIME_SIG_CUT, "timeSigCutCommon", stave, ctx)
            else -> drawNumeric(stave, ctx)
        }
    }

    private fun drawCommonGlyph(codepoint: Int, glyphName: String, stave: VFStave, ctx: VexRenderingContext) {
        val centerY = stave.getYForLine(2f)
        val raw = VFGlyphBoundingBoxManager.get(glyphName)
        if (raw == null) {
            ctx.drawSmuflGlyph(codepoint, x, centerY, sizePx)
            return
        }
        val scaled = raw.scaled(stave.spacingBetweenLines)
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
        val originY = centerY - centerOffsetY
        ctx.drawSmuflGlyph(codepoint, x, originY, sizePx)
    }

    private fun drawNumeric(stave: VFStave, ctx: VexRenderingContext) {
        val staffSpacing = stave.spacingBetweenLines
        var topX = x
        val topY = stave.getYForLine(1f)
        for (glyph in topGlyphs) {
            val originY = adjustedDigitOriginY(glyph, topY, stave)
            val drawX = topX - digitLeftBearingForStaffSpacing(glyph, staffSpacing)
            ctx.drawSmuflGlyph(glyph, drawX, originY, sizePx)
            topX += digitAdvanceForStaffSpacing(glyph, staffSpacing)
        }

        var bottomX = x
        val bottomY = stave.getYForLine(3f)
        for (glyph in bottomGlyphs) {
            val originY = adjustedDigitOriginY(glyph, bottomY, stave)
            val drawX = bottomX - digitLeftBearingForStaffSpacing(glyph, staffSpacing)
            ctx.drawSmuflGlyph(glyph, drawX, originY, sizePx)
            bottomX += digitAdvanceForStaffSpacing(glyph, staffSpacing)
        }
    }

    private fun digitAdvanceForStaffSpacing(codepoint: Int, staffSpacing: Float): Float {
        return digitGlyphName(codepoint)
            ?.let { VFGlyphBoundingBoxManager.get(it)?.scaled(staffSpacing)?.width }
            ?: (sizePx * 0.6f)
    }

    private fun digitLeftBearingForStaffSpacing(codepoint: Int, staffSpacing: Float): Float {
        return digitGlyphName(codepoint)
            ?.let { VFGlyphBoundingBoxManager.get(it)?.scaled(staffSpacing)?.southwest?.x }
            ?: 0f
    }

    private fun adjustedDigitOriginY(glyph: Int, centerY: Float, stave: VFStave): Float {
        val name = digitGlyphName(glyph) ?: return centerY
        val raw = VFGlyphBoundingBoxManager.get(name) ?: return centerY
        val scaled = raw.scaled(stave.spacingBetweenLines)
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
        return centerY - centerOffsetY
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
}
