package dev.pola.notewise.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: RendererViewModel = viewModel()
) {
    val context = LocalContext.current
    val errorMessage by vm.importError.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.loadFile(context, it) }
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        vm.clearImportError()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Button(
            onClick = {
                filePickerLauncher.launch(
                    arrayOf(
                        "application/vnd.recordare.musicxml+xml",
                        "application/vnd.recordare.musicxml",
                        "application/xml",
                        "text/xml"
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Open File")
        }

        RendererScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            vm = vm
        )
    }
}
