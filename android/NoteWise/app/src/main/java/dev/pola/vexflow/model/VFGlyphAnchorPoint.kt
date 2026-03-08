package dev.pola.vexflow.model

import android.graphics.PointF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.pola.notewise.App

/**
 * Optional per-glyph anchor metadata in staff-space units.
 * For clefs, yAnchor identifies the glyph-local Y that should align to the target staff line.
 */
data class VFGlyphAnchorPoint(
    val yAnchor: Float = 0f,
    val cutOutNE: PointF? = null,
    val cutOutSE: PointF? = null,
    val cutOutSW: PointF? = null,
    val cutOutNW: PointF? = null
)

object VFGlyphAnchorPointManager {

    private val anchors: Map<String, VFGlyphAnchorPoint> by lazy { load() }

    fun get(glyphName: String): VFGlyphAnchorPoint? = anchors[glyphName]

    private data class RawAnchorEntry(
        val yAnchor: Float? = null,
        val cutOutNE: List<Float>? = null,
        val cutOutSE: List<Float>? = null,
        val cutOutSW: List<Float>? = null,
        val cutOutNW: List<Float>? = null
    )

    private fun parsePoint(raw: List<Float>?): PointF? {
        if (raw == null || raw.size < 2) return null
        return PointF(raw[0], raw[1])
    }

    private fun load(): Map<String, VFGlyphAnchorPoint> {
        return try {
            val json = App.instance.assets.open("glyph_anchor_points.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, RawAnchorEntry>>() {}.type
            val raw: Map<String, RawAnchorEntry> = Gson().fromJson(json, type)
            raw.mapValues { (_, entry) ->
                VFGlyphAnchorPoint(
                    yAnchor = entry.yAnchor ?: 0f,
                    cutOutNE = parsePoint(entry.cutOutNE),
                    cutOutSE = parsePoint(entry.cutOutSE),
                    cutOutSW = parsePoint(entry.cutOutSW),
                    cutOutNW = parsePoint(entry.cutOutNW)
                )
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
