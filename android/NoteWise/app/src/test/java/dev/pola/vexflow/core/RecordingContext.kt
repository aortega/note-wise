package dev.pola.vexflow.core

/**
 * Lightweight test double that records primitive draw calls.
 */
class RecordingContext : VexRenderingContext() {
    data class GlyphCall(val codepoint: Int, val x: Float, val y: Float, val size: Float)

    val fillRectCalls = mutableListOf<FloatArray>()
    val strokeCalls = mutableListOf<Unit>()
    val glyphCalls = mutableListOf<GlyphCall>()

    override fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        fillRectCalls += floatArrayOf(x, y, width, height)
    }

    override fun stroke() {
        strokeCalls += Unit
    }

    override fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
        glyphCalls += GlyphCall(codepoint, x, y, sizePx)
    }

    override fun save() {}
    override fun restore() {}
    override fun translate(x: Float, y: Float) {}
    override fun scale(sx: Float, sy: Float) {}
}
