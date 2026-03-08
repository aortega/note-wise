package dev.pola.notewise.renderer

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileImportHandlerTest {

    private fun resolveRepoSample(pathFromAndroidRoot: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        val cwd = File(userDir).canonicalFile
        val candidates = listOf(
            File(cwd, "../$pathFromAndroidRoot"),
            File(cwd, "../../$pathFromAndroidRoot"),
            File(cwd, pathFromAndroidRoot),
            File(cwd, "android/$pathFromAndroidRoot")
        ).map { it.canonicalFile }
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    @Test
    fun `importFile parses repository xml sample via uri`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sample = resolveRepoSample("samples/lilypond_tests/xml_files/01a-Pitches-Pitches.xml")
        assertTrue(sample.exists())

        val result = FileImportHandler.importFile(context, Uri.fromFile(sample))

        assertNotNull(result)
        assertTrue(result!!.parts.isNotEmpty())
        assertTrue(result.parts.first().measures.isNotEmpty())
    }
}
