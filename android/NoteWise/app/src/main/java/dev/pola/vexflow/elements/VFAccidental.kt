package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphAnchorPointManager
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables

/**
 * A single accidental drawn to the left of a notehead.
 */
class VFAccidental(
    val type: AccidentalType,
    val noteLineIndex: Int,
    val displayOptions: DisplayOptions = DisplayOptions()
) {
    data class DisplayOptions(
        val cautionary: Boolean = false,
        val editorial: Boolean = false,
        val parenthesized: Boolean = false,
        val bracketed: Boolean = false
    ) {
        fun isDecorated(): Boolean = parenthesized || bracketed
    }

    private data class GlyphToken(
        val codepoint: Int,
        val glyphName: String,
        val isBaseGlyph: Boolean = false
    )

    private data class GlyphMetrics(
        val width: Float,
        val centerOffsetX: Float,
        val anchorOffsetY: Float
    )

    var x: Float = 0f
    var staveY: Float = 0f
    var staffLineSpacing: Float = 10f

    private val glyphSizePx: Float get() = staffLineSpacing * 4f

    private fun isMicrotoneType(type: AccidentalType): Boolean {
        return when (type) {
            AccidentalType.QUARTER_FLAT,
            AccidentalType.QUARTER_SHARP,
            AccidentalType.HALF_SHARP,
            AccidentalType.THREE_QUARTER_FLAT,
            AccidentalType.THREE_QUARTER_SHARP -> true
            else -> false
        }
    }

    private fun useCenteredYAnchor(type: AccidentalType): Boolean {
        return when (type) {
            // Only apply to microtone flat-family symbols. Standard flats use metadata
            // anchors to remain compatible with approved baseline fixtures.
            AccidentalType.QUARTER_FLAT,
            AccidentalType.THREE_QUARTER_FLAT -> true
            else -> false
        }
    }

    private fun verticalNudgePx(type: AccidentalType): Float {
        return when (type) {
            // Quarter-flat and three-quarter-flat glyphs sit visually low at baseline
            // alignment; lift them by half a staff-space.
            AccidentalType.QUARTER_FLAT,
            AccidentalType.THREE_QUARTER_FLAT -> -(staffLineSpacing * 0.5f)
            else -> 0f
        }
    }

    private fun tokenGapPx(): Float = staffLineSpacing * 0.08f

    private fun glyphTokens(): List<GlyphToken> {
        val tokens = mutableListOf<GlyphToken>()
        if (displayOptions.bracketed) {
            tokens += GlyphToken(
                codepoint = VFTables.GLYPH_ACCIDENTAL_BRACKET_LEFT,
                glyphName = "accidentalBracketLeft"
            )
        }
        if (displayOptions.parenthesized) {
            tokens += GlyphToken(
                codepoint = VFTables.GLYPH_ACCIDENTAL_PARENS_LEFT,
                glyphName = "accidentalParensLeft"
            )
        }
        tokens += GlyphToken(
            codepoint = type.codepoint,
            glyphName = type.glyphName,
            isBaseGlyph = true
        )
        if (displayOptions.parenthesized) {
            tokens += GlyphToken(
                codepoint = VFTables.GLYPH_ACCIDENTAL_PARENS_RIGHT,
                glyphName = "accidentalParensRight"
            )
        }
        if (displayOptions.bracketed) {
            tokens += GlyphToken(
                codepoint = VFTables.GLYPH_ACCIDENTAL_BRACKET_RIGHT,
                glyphName = "accidentalBracketRight"
            )
        }
        return tokens
    }

    private fun tokenFallbackWidthPx(token: GlyphToken): Float {
        return when (token.glyphName) {
            "accidentalParensLeft", "accidentalParensRight" -> staffLineSpacing * 0.56f
            "accidentalBracketLeft", "accidentalBracketRight" -> staffLineSpacing * 0.31f
            else -> when (type) {
                AccidentalType.QUARTER_FLAT,
                AccidentalType.QUARTER_SHARP,
                AccidentalType.HALF_SHARP,
                AccidentalType.THREE_QUARTER_FLAT,
                AccidentalType.THREE_QUARTER_SHARP -> staffLineSpacing * 1.15f
                else -> staffLineSpacing * 0.75f
            }
        }
    }

    private fun glyphMetrics(token: GlyphToken, ctx: VexRenderingContext?): GlyphMetrics {
        val raw = runCatching { VFGlyphBoundingBoxManager.get(token.glyphName) }.getOrNull()
        if (raw != null) {
            val scaled = raw.scaled(staffLineSpacing)
            val centerOffsetX = (scaled.northeast.x + scaled.southwest.x) / 2f
            val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
            val metadataAnchorOffsetY =
                runCatching { VFGlyphAnchorPointManager.get(token.glyphName) }
                    .getOrNull()
                    ?.yAnchor
                    ?.times(staffLineSpacing)
            val useCentered = token.isBaseGlyph && useCenteredYAnchor(type)
            val anchorOffsetY = if (useCentered) centerOffsetY else (metadataAnchorOffsetY ?: centerOffsetY)

            return GlyphMetrics(
                width = scaled.width.coerceAtLeast(staffLineSpacing * 0.25f),
                centerOffsetX = centerOffsetX,
                anchorOffsetY = anchorOffsetY
            )
        }

        val measured = ctx?.measureSmuflGlyphBounds(token.codepoint, glyphSizePx)
        if (measured != null) {
            val width = (measured.right - measured.left).coerceAtLeast(staffLineSpacing * 0.25f)
            val centerOffsetX = (measured.left + measured.right) / 2f
            val centerOffsetY = (measured.top + measured.bottom) / 2f
            return GlyphMetrics(
                width = width,
                centerOffsetX = centerOffsetX,
                anchorOffsetY = centerOffsetY
            )
        }

        val fallbackWidth = tokenFallbackWidthPx(token)
        return GlyphMetrics(
            width = fallbackWidth,
            centerOffsetX = fallbackWidth / 2f,
            anchorOffsetY = 0f
        )
    }

    fun approximateWidthPx(ctx: VexRenderingContext?): Float {
        val tokens = glyphTokens()
        if (tokens.isEmpty()) return 0f
        val widths = tokens.map { glyphMetrics(it, ctx).width }
        return widths.sum() + tokenGapPx() * (widths.size - 1).coerceAtLeast(0)
    }

    fun horizontalBoundsPx(centerX: Float, ctx: VexRenderingContext): Pair<Float, Float> {
        val width = approximateWidthPx(ctx)
        val left = centerX - (width / 2f)
        return left to (left + width)
    }

    fun draw(ctx: VexRenderingContext) {
        val desiredAnchorY = staveY + noteLineIndex * (staffLineSpacing / 2f) + verticalNudgePx(type)
        val tokens = glyphTokens()
        if (tokens.isEmpty()) return

        val metrics = tokens.map { glyphMetrics(it, ctx) }
        val gapPx = tokenGapPx()
        val totalWidth = metrics.sumOf { it.width.toDouble() }.toFloat() +
            gapPx * (metrics.size - 1).coerceAtLeast(0)

        var leftCursor = x - (totalWidth / 2f)
        for (index in tokens.indices) {
            val token = tokens[index]
            val tokenMetrics = metrics[index]
            val centerX = leftCursor + (tokenMetrics.width / 2f)
            val originX = centerX - tokenMetrics.centerOffsetX
            val originY = desiredAnchorY - tokenMetrics.anchorOffsetY
            ctx.drawSmuflGlyph(token.codepoint, originX, originY, glyphSizePx)
            leftCursor += tokenMetrics.width + gapPx
        }
    }

    enum class AccidentalType(val codepoint: Int, val glyphName: String) {
        SHARP(VFTables.GLYPH_ACCIDENTAL_SHARP, "accidentalSharp"),
        FLAT(VFTables.GLYPH_ACCIDENTAL_FLAT, "accidentalFlat"),
        NATURAL(VFTables.GLYPH_ACCIDENTAL_NATURAL, "accidentalNatural"),
        DOUBLE_SHARP(VFTables.GLYPH_ACCIDENTAL_DOUBLE_SHARP, "accidentalDoubleSharp"),
        DOUBLE_FLAT(VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT, "accidentalDoubleFlat"),
        QUARTER_FLAT(VFTables.GLYPH_ACCIDENTAL_QUARTER_FLAT, "accidentalQuarterToneFlatStein"),
        QUARTER_SHARP(VFTables.GLYPH_ACCIDENTAL_QUARTER_SHARP, "accidentalQuarterToneSharpStein"),
        HALF_SHARP(VFTables.GLYPH_ACCIDENTAL_HALF_SHARP, "accidentalQuarterToneSharpStein"),
        THREE_QUARTER_FLAT(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT, "accidentalThreeQuarterTonesFlatZimmermann"),
        THREE_QUARTER_SHARP(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP, "accidentalThreeQuarterTonesSharpStein");

        companion object {
            fun fromString(s: String): AccidentalType? = when (s) {
                "#" -> SHARP
                "b" -> FLAT
                "n" -> NATURAL
                "##" -> DOUBLE_SHARP
                "bb" -> DOUBLE_FLAT
                "qb" -> QUARTER_FLAT                    // quarter-flat (also used for half-flat fallback)
                "qs" -> QUARTER_SHARP                   // quarter-sharp
                "#h" -> HALF_SHARP                      // half-sharp
                "db" -> THREE_QUARTER_FLAT              // three-quarter-flat (three-quarter-tone flat)
                "#t" -> THREE_QUARTER_SHARP             // three-quarter-sharp (three-quarter-tone sharp)
                else -> null
            }
        }
    }
}
