package dev.pola.vexflow.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import dev.pola.notewise.App

/**
 * Abstraction over Android Canvas + Paint.
 */
open class VexRenderingContext {

    data class DrawnGlyphBox(
        val codepoint: Int,
        val measureNumber: Int?,
        val staffNumber: Int?,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    var canvas: Any? = null
        set(value) {
            field = value
            _canvas = value as? Canvas
        }

    private var _canvas: Canvas? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 1f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 14f
    }

    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val glyphAnchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    var debugDrawGlyphAnchors: Boolean = false
    var debugCollectGlyphBoxes: Boolean = false
    var debugGlyphMeasureNumber: Int? = null
    var debugGlyphStaffNumber: Int? = null
    private val debugGlyphBoxes = mutableListOf<DrawnGlyphBox>()

    var fillColor: Int = Color.BLACK
        set(value) {
            field = value
            fillPaint.color = value
            glyphPaint.color = value
        }

    var strokeColor: Int = Color.BLACK
        set(value) {
            field = value
            strokePaint.color = value
        }

    var lineWidth: Float = 1f
        set(value) {
            field = value
            strokePaint.strokeWidth = value
        }

    var alpha: Float = 1f
        set(value) {
            field = value
            val a = (value.coerceIn(0f, 1f) * 255).toInt()
            fillPaint.alpha = a
            strokePaint.alpha = a
            glyphPaint.alpha = a
        }

    private val bravuraTypeface: Typeface by lazy {
        Typeface.createFromAsset(App.instance.assets, "fonts/Bravura.otf")
            ?: error("Bravura.otf not found in assets/fonts/")
    }

    private var currentPath = Path()

    open fun beginPath() {
        currentPath = Path()
    }

    open fun moveTo(x: Float, y: Float) {
        currentPath.moveTo(x, y)
    }

    open fun lineTo(x: Float, y: Float) {
        currentPath.lineTo(x, y)
    }

    open fun quadraticCurveTo(cpx: Float, cpy: Float, x: Float, y: Float) {
        currentPath.quadTo(cpx, cpy, x, y)
    }

    open fun bezierCurveTo(
        cp1x: Float, cp1y: Float,
        cp2x: Float, cp2y: Float,
        x: Float, y: Float
    ) {
        currentPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
    }

    open fun arc(
        x: Float, y: Float, radius: Float,
        startAngle: Float, endAngle: Float, anticlockwise: Boolean
    ) {
        val left = x - radius
        val top = y - radius
        val right = x + radius
        val bottom = y + radius
        val startDeg = Math.toDegrees(startAngle.toDouble()).toFloat()
        var sweepDeg = Math.toDegrees((endAngle - startAngle).toDouble()).toFloat()
        if (anticlockwise && sweepDeg > 0) sweepDeg -= 360f
        if (!anticlockwise && sweepDeg < 0) sweepDeg += 360f
        currentPath.arcTo(RectF(left, top, right, bottom), startDeg, sweepDeg)
    }

    open fun closePath() {
        currentPath.close()
    }

    open fun stroke() {
        _canvas?.drawPath(currentPath, strokePaint)
    }

    open fun fill() {
        _canvas?.drawPath(currentPath, fillPaint)
    }

    open fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        _canvas?.drawRect(x, y, x + width, y + height, fillPaint)
    }

    open fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        _canvas?.drawRect(x, y, x + width, y + height, strokePaint)
    }

    open fun fillText(text: String, x: Float, y: Float) {
        _canvas?.drawText(text, x, y, textPaint)
    }

    open fun measureText(text: String): Float = textPaint.measureText(text)

    fun setFontSize(sizePx: Float) {
        textPaint.textSize = sizePx
    }

    open fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
        val c = _canvas ?: return
        glyphPaint.typeface = bravuraTypeface
        glyphPaint.textSize = sizePx
        val glyphStr = String(Character.toChars(codepoint))

        if (debugCollectGlyphBoxes) {
            val raw = Rect()
            glyphPaint.getTextBounds(glyphStr, 0, glyphStr.length, raw)
            debugGlyphBoxes += DrawnGlyphBox(
                codepoint = codepoint,
                measureNumber = debugGlyphMeasureNumber,
                staffNumber = debugGlyphStaffNumber,
                left = x + raw.left,
                top = y + raw.top,
                right = x + raw.right,
                bottom = y + raw.bottom
            )
        }

        c.drawText(glyphStr, x, y, glyphPaint)
        if (debugDrawGlyphAnchors) {
            val r = (sizePx * 0.06f).coerceIn(2f, 8f)
            c.drawCircle(x, y, r, glyphAnchorPaint)
        }
    }

    fun consumeDebugGlyphBoxes(): List<DrawnGlyphBox> {
        val snapshot = debugGlyphBoxes.toList()
        debugGlyphBoxes.clear()
        return snapshot
    }

    open fun save() {
        _canvas?.save()
    }

    open fun restore() {
        _canvas?.restore()
    }

    open fun translate(x: Float, y: Float) {
        _canvas?.translate(x, y)
    }

    open fun scale(sx: Float, sy: Float) {
        _canvas?.scale(sx, sy)
    }
}
