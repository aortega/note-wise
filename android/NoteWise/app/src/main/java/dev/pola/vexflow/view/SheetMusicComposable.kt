package dev.pola.vexflow.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.elements.VFStave

/**
 * Compose wrapper around [SheetMusicView].
 */
@Composable
fun SheetMusicComposable(
    stave: VFStave,
    voices: List<VFVoice>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SheetMusicView(context).also { view ->
                view.setVoices(voices, stave)
            }
        },
        update = { view ->
            view.setVoices(voices, stave)
        },
        modifier = modifier
    )
}
