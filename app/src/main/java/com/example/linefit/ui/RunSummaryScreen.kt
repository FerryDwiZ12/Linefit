package com.example.linefit.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linefit.data.ActivitySummary
import com.example.linefit.renderer.AspectRatioOption
import com.example.linefit.renderer.MercatorProjector
import com.example.linefit.renderer.NeonGlowTheme
import com.example.linefit.renderer.NeonRouteRenderer
import com.example.linefit.renderer.RouteStyleConfig
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

    var showCustomizeSheet by remember { mutableStateOf(false) }

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
        bottomBar = {
            StudioBottomActionBar(
                onImportClicked = onImportGpxClicked,
                onCustomizeClicked = { showCustomizeSheet = true },
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
            Spacer(modifier = Modifier.height(12.dp))

            // 1. App Header Title & Brand Sub-label
            Text(
                text = "PATHCRAFT STUDIO",
                style = MaterialTheme.typography.labelMedium,
                color = VoltLime,
                letterSpacing = 2.sp
            )

            // 2. Top Metric: Distance Header
            TopDistanceHeader(summary = summary)

            // 3. Middle Section: Grid (Route Preview + Time & Pace Cards)
            MiddlePreviewGrid(
                summary = summary,
                routeConfig = routeConfig
            )

            // 4. Bottom Section: Split Comparison Bar Chart Card
            SplitComparisonCard(summary = summary)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCustomizeSheet) {
        CustomizeOverlayBottomSheet(
            config = routeConfig,
            viewModel = viewModel,
            onDismiss = { showCustomizeSheet = false }
        )
    }
}

@Composable
private fun TopDistanceHeader(summary: ActivitySummary) {
    Column {
        Text(
            text = String.format(Locale.US, "%.2f", summary.totalDistanceKm),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Text(
            text = "KILOMETERS COMPLETED",
            style = MaterialTheme.typography.labelMedium,
            color = VoltLime,
            letterSpacing = 1.8.sp
        )
    }
}

@Composable
private fun MiddlePreviewGrid(
    summary: ActivitySummary,
    routeConfig: RouteStyleConfig
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column: Neon Route Canvas Preview Card (60% width)
        Card(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
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
                            config = routeConfig.copy(
                                showStatsOverlay = false,
                                showSplitsChart = false,
                                showMiniRouteCard = false
                            ),
                            isTransparentBg = true
                        )
                    }
                }
            }
        }

        // Right Column: Time Card & Pace Card (40% width)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Time Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.durationFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            }

            // Avg Pace Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AVG PACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.avgPaceFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitComparisonCard(summary: ActivitySummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "SPLIT COMPARISON",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            val splits = summary.splits
            if (splits.isNotEmpty()) {
                val maxPace = splits.maxOfOrNull { it.avgPaceSecPerKm } ?: 300.0
                val minPace = splits.minOfOrNull { it.avgPaceSecPerKm } ?: 200.0
                val paceRange = (maxPace - minPace).coerceAtLeast(1.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    splits.take(10).forEach { split ->
                        val normalizedRatio = 0.35f + 0.65f * ((maxPace - split.avgPaceSecPerKm) / paceRange).toFloat()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Bar Pill
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(normalizedRatio)
                                    .clip(CircleShape)
                                    .background(if (split.isFastest) VoltLime else MutedOlive)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Km Index Label
                            Text(
                                text = "${split.kmIndex}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (split.isFastest) VoltLime else TextSecondary,
                                fontWeight = if (split.isFastest) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioBottomActionBar(
    onImportClicked: () -> Unit,
    onCustomizeClicked: () -> Unit,
    onExportClicked: () -> Unit,
    isExporting: Boolean
) {
    Surface(
        color = BackgroundDark,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onImportClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "IMPORT GPX", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onCustomizeClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltLime),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "CUSTOMIZE", style = MaterialTheme.typography.labelMedium)
                }
            }

            Button(
                onClick = onExportClicked,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoltLime,
                    contentColor = BackgroundDark
                ),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeOverlayBottomSheet(
    config: RouteStyleConfig,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "OVERLAY CUSTOMIZATION",
                style = MaterialTheme.typography.titleLarge,
                color = VoltLime
            )

            // 1. Glow Color Theme Palette
            Text(text = "NEON GLOW THEME", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NeonGlowTheme.entries.forEach { theme ->
                    val selected = config.glowTheme == theme
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.glowColor)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.updateGlowTheme(theme) }
                    )
                }
            }

            // 2. Stroke Width Slider
            Text(text = "LINE THICKNESS (${config.strokeWidthPx.toInt()} px)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = config.strokeWidthPx,
                onValueChange = { viewModel.updateStrokeWidth(it) },
                valueRange = 4f..30f,
                colors = SliderDefaults.colors(
                    thumbColor = VoltLime,
                    activeTrackColor = VoltLime,
                    inactiveTrackColor = CardBorder
                )
            )

            // 3. Glow Radius Slider
            Text(text = "GLOW INTENSITY (${config.glowRadiusPx.toInt()} px)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = config.glowRadiusPx,
                onValueChange = { viewModel.updateGlowRadius(it) },
                valueRange = 0f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = VoltLime,
                    activeTrackColor = VoltLime,
                    inactiveTrackColor = CardBorder
                )
            )

            // 4. Aspect Ratio Choices
            Text(text = "EXPORT ASPECT RATIO", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        Text(
                            text = ratio.label.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 5. Overlay Element Toggles
            Text(text = "OVERLAY ELEMENTS", style = MaterialTheme.typography.labelMedium)
            ToggleRow(
                label = "Show Distance/Time Stats Header",
                checked = config.showStatsOverlay,
                onCheckedChange = { viewModel.toggleStatsOverlay(it) }
            )
            ToggleRow(
                label = "Show Split Comparison Chart",
                checked = config.showSplitsChart,
                onCheckedChange = { viewModel.toggleSplitsChart(it) }
            )
            ToggleRow(
                label = "Show Route Border Frame",
                checked = config.showMiniRouteCard,
                onCheckedChange = { viewModel.toggleMiniRouteCard(it) }
            )
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
