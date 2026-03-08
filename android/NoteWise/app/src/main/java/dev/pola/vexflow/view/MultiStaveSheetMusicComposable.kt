package dev.pola.vexflow.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * Compose wrapper around [MultiStaveSheetMusicView].
 */
@Composable
fun MultiStaveSheetMusicComposable(
    measures: List<MusicSheetToVF.RenderedMeasure>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            MultiStaveSheetMusicView(context).also { view ->
                view.setMeasures(measures)
            }
        },
        update = { view ->
            view.setMeasures(measures)
        },
        modifier = modifier
    )
}
