package com.example.linefit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.linefit.ui.MainViewModel
import com.example.linefit.ui.RunSummaryScreen
import com.example.linefit.ui.theme.BackgroundDark
import com.example.linefit.ui.theme.LinefitTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LinefitTheme {
                val gpxPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { viewModel.loadGpxFromUri(this, it) }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    RunSummaryScreen(
                        viewModel = viewModel,
                        onImportGpxClicked = {
                            gpxPickerLauncher.launch("*/*")
                        }
                    )
                }
            }
        }
    }
}
