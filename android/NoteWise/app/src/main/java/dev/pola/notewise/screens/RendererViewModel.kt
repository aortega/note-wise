package dev.pola.notewise.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pola.notewise.renderer.FileImportHandler
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.elements.VFBarline
import dev.pola.vexflow.elements.VFBarlineType
import dev.pola.vexflow.elements.VFClef
import dev.pola.vexflow.elements.VFKeySignature
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.elements.VFTimeSignature
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import dev.pola.vexflow.parser.MusicSheet
import dev.pola.vexflow.parser.MusicSheetToVF
import dev.pola.vexflow.parser.MusicXMLParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RendererViewModel : ViewModel() {

    data class ScoreViewData(
        val title: String,
        val measures: List<MusicSheetToVF.RenderedMeasure>
    )

    private val _musicSheet = MutableStateFlow<MusicSheet?>(null)
    val musicSheet: StateFlow<MusicSheet?> = _musicSheet.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val parsed = FileImportHandler.importFile(context, uri)
            if (parsed == null) {
                _importError.value = "Could not parse file. Please choose a valid MusicXML (.xml/.mxl)."
                return@launch
            }
            _musicSheet.value = parsed
            _importError.value = null
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun buildScoreData(context: Context, staveWidthPx: Float, selectedSheet: MusicSheet?): ScoreViewData {
        return if (selectedSheet != null) {
            renderSheet(selectedSheet, staveWidthPx, "Imported MusicXML")
        } else {
            buildM8SampleScore(context, staveWidthPx)
        }
    }

    fun buildM8SampleScore(context: Context, staveWidthPx: Float): ScoreViewData {
        return try {
            val parser = MusicXMLParser()
            val clairDeLuneSheet =
                context.assets.open("samples/Clair_de_lune_-_Claude_Debussy.mxl")
                    .use { parser.parse(it) }

            renderSheet(clairDeLuneSheet, staveWidthPx, "Clair de Lune (Full)")
        } catch (_: Exception) {
            ScoreViewData(
                title = "Renderer (M8 fallback demo)",
                measures = listOf(buildDemoScore(staveWidthPx))
            )
        }
    }

    private fun renderSheet(sheet: MusicSheet, staveWidthPx: Float, fallbackTitle: String): ScoreViewData {
        val rendered = MusicSheetToVF.convert(
            sheet = sheet,
            startX = 20f,
            startY = 80f,
            staveWidth = (staveWidthPx - 40f).coerceAtLeast(240f)
        )

        val title = sheet.title.ifBlank { fallbackTitle }

        return ScoreViewData(
            title = title,
            measures = if (rendered.isNotEmpty()) rendered else listOf(buildDemoScore(staveWidthPx))
        )
    }

    fun buildDemoScore(staveWidthPx: Float): MusicSheetToVF.RenderedMeasure {
        val staffSpacing = 16f
        val noteGlyphScale = staffSpacing * 4f
        val clefSize = staffSpacing * 4.5f
        val keySigSize = staffSpacing * 3.2f
        val timeSigSize = staffSpacing * 4f

        val stave = VFStave(
            x = 20f,
            y = 80f,
            width = (staveWidthPx - 40f).coerceAtLeast(240f),
            options = VFStaveOptions(spacingBetweenLinesPx = staffSpacing)
        ).apply {
            clef = VFClef("treble").apply { sizePx = clefSize }
            keySignature = VFKeySignature("G").apply { sizePx = keySigSize }
            timeSignature = VFTimeSignature("4/4").apply { sizePx = timeSigSize }
            startBarline = VFBarline(VFBarlineType.SINGLE)
            endBarline = VFBarline(VFBarlineType.END)
            lineThickness = 1.8f
        }

        val voice = VFVoice("4/4")
        voice.addTickables(
            listOf(
                VFStaveNote(VFStaveNoteStruct(keys = listOf("f/5"), duration = "4", glyphFontScale = noteGlyphScale)),
                VFStaveNote(VFStaveNoteStruct(keys = listOf("g/5"), duration = "4", glyphFontScale = noteGlyphScale)),
                VFStaveNote(VFStaveNoteStruct(keys = listOf("a/5"), duration = "4", glyphFontScale = noteGlyphScale)),
                VFStaveNote(VFStaveNoteStruct(keys = listOf("b/5"), duration = "4", glyphFontScale = noteGlyphScale))
            )
        )

        return MusicSheetToVF.RenderedMeasure(
            measureNumber = 1,
            staves = listOf(
                MusicSheetToVF.RenderedStaff(
                    staffNumber = 1,
                    resolvedClefType = "treble",
                    stave = stave,
                    voices = listOf(voice),
                    beams = emptyList(),
                    ties = emptyList()
                )
            )
        )
    }
}
