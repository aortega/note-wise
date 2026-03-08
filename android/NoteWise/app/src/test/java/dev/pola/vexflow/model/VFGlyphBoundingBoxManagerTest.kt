package dev.pola.vexflow.model

import androidx.test.core.app.ApplicationProvider
import dev.pola.notewise.App
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [33])
class VFGlyphBoundingBoxManagerTest {

    @Test
    fun glyphBboxLoads() {
        ApplicationProvider.getApplicationContext<App>()
        val gClef = VFGlyphBoundingBoxManager.get("gClef")
        assertNotNull(gClef)
    }
}
