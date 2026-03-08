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
    val noteLineIndex: Int
) {
    var x: Float = 0f
    var staveY: Float = 0f
    var staffLineSpacing: Float = 10f

    private val glyphSizePx: Float get() = staffLineSpacing * 4f

    fun draw(ctx: VexRenderingContext) {
        val desiredAnchorY = staveY + noteLineIndex * (staffLineSpacing / 2f)
        val glyphName = type.glyphName
        val raw = VFGlyphBoundingBoxManager.get(glyphName)
        if (raw == null) {
            ctx.drawSmuflGlyph(type.codepoint, x, desiredAnchorY, glyphSizePx)
            return
        }

        val scaled = raw.scaled(staffLineSpacing)
        val centerOffsetX = (scaled.northeast.x + scaled.southwest.x) / 2f
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
        val metadataAnchorOffsetY =
            VFGlyphAnchorPointManager.get(glyphName)?.yAnchor?.times(staffLineSpacing)
        val glyphAnchorOffsetY = metadataAnchorOffsetY ?: centerOffsetY

        val originX = x - centerOffsetX
        val originY = desiredAnchorY - glyphAnchorOffsetY
        ctx.drawSmuflGlyph(type.codepoint, originX, originY, glyphSizePx)
    }

    enum class AccidentalType(val codepoint: Int, val glyphName: String) {
        SHARP(VFTables.GLYPH_ACCIDENTAL_SHARP, "accidentalSharp"),
        FLAT(VFTables.GLYPH_ACCIDENTAL_FLAT, "accidentalFlat"),
        NATURAL(VFTables.GLYPH_ACCIDENTAL_NATURAL, "accidentalNatural"),
        DOUBLE_SHARP(VFTables.GLYPH_ACCIDENTAL_DOUBLE_SHARP, "accidentalDoubleSharp"),
        DOUBLE_FLAT(VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT, "accidentalDoubleFlat");

        companion object {
            fun fromString(s: String): AccidentalType? = when (s) {
                "#" -> SHARP
                "b" -> FLAT
                "n" -> NATURAL
                "##" -> DOUBLE_SHARP
                "bb" -> DOUBLE_FLAT
                else -> null
            }
        }
    }
}
