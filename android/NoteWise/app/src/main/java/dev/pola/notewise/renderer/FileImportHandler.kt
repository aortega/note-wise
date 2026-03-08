package dev.pola.notewise.renderer

import android.content.Context
import android.net.Uri
import dev.pola.vexflow.parser.MusicSheet
import dev.pola.vexflow.parser.MusicXMLParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileImportHandler {

    /**
     * Parses a Storage Access Framework URI as MusicXML or MXL.
     * Returns null on parse failure or unreadable stream.
     */
    suspend fun importFile(context: Context, uri: Uri): MusicSheet? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    MusicXMLParser().parse(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
}
