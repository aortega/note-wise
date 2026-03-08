package dev.pola.vexflow.model

import android.graphics.PointF
import androidx.test.core.app.ApplicationProvider
import dev.pola.notewise.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [33])
class VFGlyphBoundingBoxTest {

    @Test
    fun `manager loads gClef bounding box from assets`() {
        ApplicationProvider.getApplicationContext<App>()
        val box = VFGlyphBoundingBoxManager.get("gClef")
        assertNotNull(box)
    }

    @Test
    fun `scaled bounding box width changes proportionally`() {
        val box = VFGlyphBoundingBox(
            northeast = PointF(2f, 1f),
            southwest = PointF(0f, -1f)
        )

        val scaled = box.scaled(10f)

        assertEquals(box.width * 10f, scaled.width, 0.001f)
    }

    @Test
    fun `toCanvasRect flips Y axis`() {
        val box = VFGlyphBoundingBox(
            northeast = PointF(1f, 2f),
            southwest = PointF(0f, -1f)
        )

        val rect = box.toCanvasRect(originX = 10f, originY = 20f, staffSpacing = 10f)

        assertEquals(10f, rect.left, 0.001f)
        assertEquals(0f, rect.top, 0.001f)
        assertEquals(20f, rect.right, 0.001f)
        assertEquals(30f, rect.bottom, 0.001f)
    }
}
