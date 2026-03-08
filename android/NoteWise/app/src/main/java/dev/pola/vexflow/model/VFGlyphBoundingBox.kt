package dev.pola.vexflow.model

import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.pola.notewise.App

/**
 * Bounding box for a single SMuFL glyph, in staff-space units.
 */
data class VFGlyphBoundingBox(
    val northeast: PointF,
    val southwest: PointF
) {
    val width: Float get() = northeast.x - southwest.x
    val height: Float get() = northeast.y - southwest.y

    fun scaled(staffSpacing: Float): VFGlyphBoundingBox = VFGlyphBoundingBox(
        northeast = PointF(northeast.x * staffSpacing, northeast.y * staffSpacing),
        southwest = PointF(southwest.x * staffSpacing, southwest.y * staffSpacing)
    )

    fun toCanvasRect(originX: Float, originY: Float, staffSpacing: Float): RectF {
        val scaled = scaled(staffSpacing)
        return RectF(
            originX + scaled.southwest.x,
            originY - scaled.northeast.y,
            originX + scaled.northeast.x,
            originY - scaled.southwest.y
        )
    }
}

object VFGlyphBoundingBoxManager {

    private val boxes: Map<String, VFGlyphBoundingBox> by lazy { load() }

    fun get(glyphName: String): VFGlyphBoundingBox? = boxes[glyphName]

    fun getScaled(glyphName: String, staffSpacing: Float): VFGlyphBoundingBox? =
        get(glyphName)?.scaled(staffSpacing)

    val availableGlyphs: List<String> get() = boxes.keys.sorted()

    private data class RawEntry(val bBoxNE: List<Float>, val bBoxSW: List<Float>)

    private fun load(): Map<String, VFGlyphBoundingBox> {
        val json = App.instance.assets.open("glyph_bboxes.json")
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, RawEntry>>() {}.type
        val raw: Map<String, RawEntry> = Gson().fromJson(json, type)
        return raw.mapValues { (_, entry) ->
            VFGlyphBoundingBox(
                northeast = PointF(entry.bBoxNE[0], entry.bBoxNE[1]),
                southwest = PointF(entry.bBoxSW[0], entry.bBoxSW[1])
            )
        }
    }
}
