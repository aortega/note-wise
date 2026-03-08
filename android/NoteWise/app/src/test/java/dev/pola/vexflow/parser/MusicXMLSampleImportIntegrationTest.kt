package dev.pola.vexflow.parser

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MusicXMLSampleImportIntegrationTest {

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
    fun `can parse lilypond xml sample from repository`() {
        val sample = resolveRepoSample("samples/lilypond_tests/xml_files/01a-Pitches-Pitches.xml")
        assertTrue("Sample file should exist: ${sample.absolutePath}", sample.exists())

        val sheet = sample.inputStream().use { MusicXMLParser().parse(it) }

        assertTrue(sheet.parts.isNotEmpty())
        assertEquals("P1", sheet.parts.first().id)
        assertTrue(sheet.parts.first().measures.size >= 2)
        assertTrue(sheet.parts.first().measures.first().notes.isNotEmpty())
    }

    @Test
    fun `can parse mxl sample from repository`() {
        val sample = resolveRepoSample("samples/lilypond_tests/xml_files/90a-Compressed-MusicXML.mxl")
        assertTrue("MXL sample should exist: ${sample.absolutePath}", sample.exists())

        val sheet = sample.inputStream().use { MusicXMLParser().parse(it) }

        assertTrue(sheet.parts.isNotEmpty())
        assertTrue(sheet.parts.first().measures.isNotEmpty())
    }
}
