package com.example.linefit.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linefit.data.ActivitySummary
import com.example.linefit.renderer.AspectRatioOption
import com.example.linefit.renderer.MercatorProjector
import com.example.linefit.renderer.NeonGlowTheme
import com.example.linefit.renderer.NeonRouteRenderer
import com.example.linefit.renderer.PresetTheme
import com.example.linefit.renderer.RoutePosition
import com.example.linefit.renderer.RouteStyleConfig
import com.example.linefit.renderer.StatsPosition
import com.example.linefit.ui.theme.BackgroundDark
import com.example.linefit.ui.theme.CardBorder
import com.example.linefit.ui.theme.MutedOlive
import com.example.linefit.ui.theme.SurfaceCard
import com.example.linefit.ui.theme.TextPrimary
import com.example.linefit.ui.theme.TextSecondary
import com.example.linefit.ui.theme.VoltLime
import com.example.linefit.utils.PngExporter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(
    viewModel: MainViewModel,
    onImportGpxClicked: () -> Unit
) {
    val context = LocalContext.current
    val summary by viewModel.activitySummary.collectAsState()
    val routeConfig by viewModel.routeConfig.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportResult.Success -> {
                Toast.makeText(context, "Overlay exported to Pictures/PathCraft!", Toast.LENGTH_LONG).show()
                PngExporter.shareImage(context, state.savedUri)
                viewModel.resetExportState()
            }
            is ExportResult.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PATHCRAFT STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = VoltLime,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 5 Interactive Studio Tabs
                StudioTabBar(
                    selectedTab = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }
        },
        bottomBar = {
            StudioExportActionBar(
                onExportClicked = { viewModel.exportPng(context) },
                isExporting = exportState is ExportResult.Loading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTabIndex) {
                0 -> TabImportAndRatio(routeConfig, viewModel, onImportGpxClicked)
                1 -> TabThemeAndStyling(routeConfig, viewModel)
                2 -> TabDataAndOverrides(summary, routeConfig, viewModel)
                3 -> TabCompositionPlacement(routeConfig, viewModel)
                4 -> TabLivePreviewAndExport(summary, routeConfig, viewModel)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StudioTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("📂 IMPORT", "🎨 THEME", "✏️ DATA", "📐 LAYOUT", "🚀 PREVIEW")
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = BackgroundDark,
        contentColor = VoltLime,
        edgePadding = 20.dp
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedTab == index) VoltLime else TextSecondary,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// Tab 1: 📂 Import & Ratio
@Composable
private fun TabImportAndRatio(
    config: RouteStyleConfig,
    viewModel: MainViewModel,
    onImportGpxClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "1. IMPORT ROUTE FILE (.GPX)", style = MaterialTheme.typography.titleLarge, color = VoltLime)
                Text(text = "Import GPX recording from Huawei Watch, Strava, or Garmin.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Button(
                    onClick = onImportGpxClicked,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SELECT GPX FILE FROM STORAGE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "2. EXPORT ASPECT RATIO", style = MaterialTheme.typography.titleLarge, color = VoltLime)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        val selected = config.aspectRatio == ratio
                        OutlinedButton(
                            onClick = { viewModel.updateAspectRatio(ratio) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) VoltLime else Color.Transparent,
                                contentColor = if (selected) BackgroundDark else TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = ratio.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// Tab 2: 🎨 Theme & Styling
@Composable
private fun TabThemeAndStyling(
    config: RouteStyleConfig,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PresetThemeCarousel(
            selectedTheme = config.presetTheme,
            onThemeSelected = { viewModel.updatePresetTheme(it) }
        )

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "NEON GLOW THEME COLOR", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NeonGlowTheme.entries.forEach { theme ->
                        val selected = config.glowTheme == theme
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.glowColor)
                                .border(width = if (selected) 3.dp else 0.dp, color = if (selected) TextPrimary else Color.Transparent, shape = CircleShape)
                                .clickable { viewModel.updateGlowTheme(theme) }
                        )
                    }
                }

                Text(text = "LINE THICKNESS (${config.strokeWidthPx.toInt()} px)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = config.strokeWidthPx,
                    onValueChange = { viewModel.updateStrokeWidth(it) },
                    valueRange = 4f..30f,
                    colors = SliderDefaults.colors(thumbColor = VoltLime, activeTrackColor = VoltLime, inactiveTrackColor = CardBorder)
                )

                Text(text = "GLOW INTENSITY (${config.glowRadiusPx.toInt()} px)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = config.glowRadiusPx,
                    onValueChange = { viewModel.updateGlowRadius(it) },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = VoltLime, activeTrackColor = VoltLime, inactiveTrackColor = CardBorder)
                )
            }
        }
    }
}

// Tab 3: ✏️ Data & Overrides
@Composable
private fun TabDataAndOverrides(
    summary: ActivitySummary,
    config: RouteStyleConfig,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "MANUAL DATA OVERRIDES (OPTIONAL)", style = MaterialTheme.typography.labelMedium, color = VoltLime)

        OutlinedTextField(
            value = config.customTitle,
            onValueChange = { viewModel.updateCustomTitle(it) },
            label = { Text("Activity Title (e.g. Morning Walk)", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = customFieldColors()
        )

        OutlinedTextField(
            value = config.customLocation,
            onValueChange = { viewModel.updateCustomLocation(it) },
            label = { Text("Location Tag (e.g. 📍 Tambakboyo)", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = customFieldColors()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = config.overrideDistanceKm?.toString() ?: String.format(Locale.US, "%.2f", summary.totalDistanceKm),
                onValueChange = { viewModel.updateOverrideDistance(it.toDoubleOrNull()) },
                label = { Text("Distance (KM)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
            OutlinedTextField(
                value = config.overrideDuration ?: summary.durationFormatted,
                onValueChange = { viewModel.updateOverrideDuration(it.ifBlank { null }) },
                label = { Text("Time (MM:SS)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = config.overridePace ?: summary.avgPaceFormatted,
                onValueChange = { viewModel.updateOverridePace(it.ifBlank { null }) },
                label = { Text("Avg Pace (/km)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
            OutlinedTextField(
                value = (config.overrideCalories ?: summary.caloriesKcal).toString(),
                onValueChange = { viewModel.updateOverrideCalories(it.toIntOrNull()) },
                label = { Text("Calories (kcal)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = (config.overrideHeartRate ?: summary.avgHeartRate ?: 154).toString(),
                onValueChange = { viewModel.updateOverrideHeartRate(it.toIntOrNull()) },
                label = { Text("Avg HR (BPM)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
            OutlinedTextField(
                value = (config.overrideElevation ?: summary.elevationGainMeters.toInt()).toString(),
                onValueChange = { viewModel.updateOverrideElevation(it.toIntOrNull()) },
                label = { Text("Elevation (m)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = customFieldColors()
            )
        }
    }
}

// Tab 4: 📐 Layout Composition
@Composable
private fun TabCompositionPlacement(
    config: RouteStyleConfig,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "ROUTE POSITION PLACEMENT", style = MaterialTheme.typography.labelMedium, color = VoltLime)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoutePosition.entries.forEach { pos ->
                        val selected = config.routePosition == pos
                        OutlinedButton(
                            onClick = { viewModel.updateRoutePosition(pos) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) VoltLime else Color.Transparent,
                                contentColor = if (selected) BackgroundDark else TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = pos.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "STATS PANEL POSITION", style = MaterialTheme.typography.labelMedium, color = VoltLime)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsPosition.entries.forEach { pos ->
                        val selected = config.statsPosition == pos
                        OutlinedButton(
                            onClick = { viewModel.updateStatsPosition(pos) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) VoltLime else Color.Transparent,
                                contentColor = if (selected) BackgroundDark else TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = pos.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "COMPONENT VISIBILITY TOGGLES", style = MaterialTheme.typography.labelMedium, color = VoltLime)
                ToggleRow("Activity Title", config.showTitle) { viewModel.toggleShowTitle(it) }
                ToggleRow("Calories Burned (kcal)", config.showCalories) { viewModel.toggleShowCalories(it) }
                ToggleRow("Avg Heart Rate (BPM)", config.showHeartRate) { viewModel.toggleShowHeartRate(it) }
                ToggleRow("Elevation Gain (m)", config.showElevation) { viewModel.toggleShowElevation(it) }
                ToggleRow("Location Tag", config.showLocationTag) { viewModel.toggleShowLocationTag(it) }
                ToggleRow("Split Comparison Chart", config.showSplitsChart) { viewModel.toggleSplitsChart(it) }
            }
        }
    }
}

// Tab 5: 🚀 Live Preview & Export
@Composable
private fun TabLivePreviewAndExport(
    summary: ActivitySummary,
    config: RouteStyleConfig,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "LIVE OVERLAY PREVIEW", style = MaterialTheme.typography.labelMedium, color = VoltLime)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val projectedPoints = MercatorProjector.projectToCanvas(
                        summary.trackPoints,
                        size.width,
                        size.height,
                        paddingPercentage = 0.15f
                    )

                    drawContext.canvas.nativeCanvas.let { nativeCanvas ->
                        NeonRouteRenderer.renderOverlay(
                            canvas = nativeCanvas,
                            width = size.width,
                            height = size.height,
                            summary = summary,
                            projectedPoints = projectedPoints,
                            config = config,
                            isTransparentBg = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetThemeCarousel(
    selectedTheme: PresetTheme,
    onThemeSelected: (PresetTheme) -> Unit
) {
    Column {
        Text(
            text = "LAYOUT PRESET (20 THEMES)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 12.dp)
        ) {
            items(PresetTheme.entries) { theme ->
                val selected = selectedTheme == theme
                Card(
                    modifier = Modifier
                        .clickable { onThemeSelected(theme) }
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) VoltLime else CardBorder,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) VoltLime.copy(alpha = 0.15f) else SurfaceCard
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) VoltLime else TextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = theme.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioExportActionBar(
    onExportClicked: () -> Unit,
    isExporting: Boolean
) {
    Surface(
        color = BackgroundDark,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onExportClicked,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = BackgroundDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isExporting) "EXPORTING..." else "EXPORT TRANSPARENT PNG",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = VoltLime,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VoltLime,
    unfocusedBorderColor = CardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard
)
