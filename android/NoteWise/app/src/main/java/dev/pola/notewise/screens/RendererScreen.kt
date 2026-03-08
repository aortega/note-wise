package dev.pola.notewise.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pola.vexflow.view.MultiStaveSheetMusicComposable

@Composable
fun RendererScreen(
    modifier: Modifier = Modifier,
    vm: RendererViewModel = viewModel()
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val importedSheet by vm.musicSheet.collectAsState()

        val scoreData = remember(maxWidth, importedSheet) {
            val widthPx = with(density) { maxWidth.toPx() }
            vm.buildScoreData(context, widthPx, importedSheet)
        }
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = scoreData.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MultiStaveSheetMusicComposable(
                measures = scoreData.measures,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
