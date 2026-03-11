package dev.pola.vexflow.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VexRenderingContextTest {

    private fun makeCtx(): Pair<VexRenderingContext, Bitmap> {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val ctx = VexRenderingContext()
        ctx.canvas = canvas
        return ctx to bitmap
    }

    private fun hasInk(bitmap: Bitmap): Boolean {
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                if (bitmap.getPixel(x, y) != Color.TRANSPARENT) return true
            }
        }
        return false
    }

    @Test
    fun `fill and stroke primitives draw on canvas`() {
        val (ctx, bitmap) = makeCtx()

        ctx.fillColor = Color.BLUE
        ctx.strokeColor = Color.RED
        ctx.lineWidth = 3f
        ctx.fillRect(10f, 20f, 80f, 10f)
        ctx.strokeRect(20f, 40f, 90f, 20f)
        ctx.fillText("VF", 30f, 90f)

        assertTrue(hasInk(bitmap))
    }

    @Test
    fun `alpha setter coerces and stores value`() {
        val (ctx, _) = makeCtx()

        ctx.alpha = 2f
        assertEquals(2f, ctx.alpha)

        ctx.alpha = -1f
        assertEquals(-1f, ctx.alpha)
    }

    @Test
    fun `path operations draw without crashing`() {
        val (ctx, _) = makeCtx()

        ctx.beginPath()
        ctx.moveTo(30f, 30f)
        ctx.lineTo(70f, 30f)
        ctx.quadraticCurveTo(90f, 50f, 70f, 70f)
        ctx.bezierCurveTo(60f, 80f, 40f, 80f, 30f, 70f)
        ctx.arc(50f, 50f, 20f, 0f, 2f, anticlockwise = false)
        ctx.arc(50f, 50f, 18f, 2f, 0f, anticlockwise = true)
        ctx.closePath()
        ctx.stroke()
        ctx.fill()

        assertTrue(true)
    }

    @Test
    fun `save restore translate and scale execute`() {
        val (ctx, bitmap) = makeCtx()

        ctx.save()
        ctx.translate(20f, 20f)
        ctx.scale(1.2f, 1.2f)
        ctx.fillRect(0f, 0f, 12f, 12f)
        ctx.restore()

        assertTrue(hasInk(bitmap))
    }

    @Test
    fun `drawSmuflGlyph draws glyph and optional anchor`() {
        val (ctx, _) = makeCtx()

        ctx.drawSmuflGlyph(0xE050, 50f, 100f, 40f)
        ctx.debugDrawGlyphAnchors = true
        ctx.drawSmuflGlyph(0xE050, 70f, 120f, 36f)

        assertTrue(ctx.debugDrawGlyphAnchors)
    }

    @Test
    fun `debug glyph box collection records primitives and tags`() {
        val (ctx, _) = makeCtx()

        ctx.debugCollectGlyphBoxes = true
        ctx.debugGlyphMeasureNumber = 12
        ctx.debugGlyphStaffNumber = 2

        ctx.fillRect(10f, 20f, 30f, 40f)
        ctx.beginPath()
        ctx.moveTo(50f, 60f)
        ctx.lineTo(90f, 60f)
        ctx.stroke()
        ctx.drawSmuflGlyph(0xE050, 70f, 120f, 36f)

        val boxes = ctx.consumeDebugGlyphBoxes()
        assertFalse(boxes.isEmpty())
        assertTrue(boxes.all { it.measureNumber == 12 })
        assertTrue(boxes.all { it.staffNumber == 2 })
    }
}
