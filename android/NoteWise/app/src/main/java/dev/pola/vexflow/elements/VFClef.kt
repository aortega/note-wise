package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphAnchorPointManager
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables

/**
 * SMuFL clef element (M3).
 */
class VFClef(
    clefType: String,
    size: String = "default",
    @Suppress("UNUSED_PARAMETER") annotation: String? = null
) {
    val type: ClefType = ClefType.fromString(clefType)

    var sizePx: Float = when (size.lowercase()) {
        "small" -> 28f
        else -> 40f
    }

    var x: Float = 0f
    val width: Float
        get() = sizePx * 1.2f

    fun widthForStaffSpacing(staffSpacing: Float): Float {
        val raw = VFGlyphBoundingBoxManager.get(type.glyphName) ?: return width
        return raw.scaled(staffSpacing).width
    }

    enum class ClefType(val glyphCodepoint: Int, val glyphName: String, val anchorMusicLine: Int) {
        TREBLE(VFTables.GLYPH_G_CLEF, "gClef", 2),
        BASS(VFTables.GLYPH_F_CLEF, "fClef", 4),
        ALTO(VFTables.GLYPH_C_CLEF, "cClef", 3),
        TENOR(VFTables.GLYPH_C_CLEF, "cClef", 4),
        SOPRANO(VFTables.GLYPH_C_CLEF, "cClef", 2),
        PERCUSSION(VFTables.GLYPH_PERCUSSION_CLEF, "unpitchedPercussionClef1", 3);

        companion object {
            fun fromString(s: String): ClefType = when (s.lowercase()) {
                "treble" -> TREBLE
                "bass" -> BASS
                "alto" -> ALTO
                "tenor" -> TENOR
                "soprano" -> SOPRANO
                "percussion" -> PERCUSSION
                else -> TREBLE
            }
        }
    }

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        val desiredAnchorY = stave.getYForMusicLine(type.anchorMusicLine)

        val raw = VFGlyphBoundingBoxManager.get(type.glyphName)
        if (raw == null) {
            ctx.drawSmuflGlyph(type.glyphCodepoint, x, desiredAnchorY, sizePx)
            return
        }

        val scaled = raw.scaled(stave.spacingBetweenLines)
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
        val metadataAnchorOffsetY =
            VFGlyphAnchorPointManager.get(type.glyphName)?.yAnchor?.times(stave.spacingBetweenLines)
        val glyphAnchorOffsetY = metadataAnchorOffsetY ?: centerOffsetY
        // Keep the glyph's visual left edge anchored at x so stave inset is exact in staff-space units.
        val originX = x - scaled.southwest.x
        val originY = desiredAnchorY - glyphAnchorOffsetY
        ctx.drawSmuflGlyph(type.glyphCodepoint, originX, originY, sizePx)
    }
}
