package com.example.linefit.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linefit.data.ActivitySummary
import com.example.linefit.data.GpxParser
import com.example.linefit.renderer.AspectRatioOption
import com.example.linefit.renderer.NeonGlowTheme
import com.example.linefit.renderer.PresetTheme
import com.example.linefit.renderer.RoutePosition
import com.example.linefit.renderer.RouteStyleConfig
import com.example.linefit.renderer.StatsPosition
import com.example.linefit.utils.PngExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportResult {
    object Idle : ExportResult
    object Loading : ExportResult
    data class Success(val savedUri: Uri) : ExportResult
    data class Error(val message: String) : ExportResult
}

class MainViewModel : ViewModel() {

    private val _activitySummary = MutableStateFlow<ActivitySummary>(GpxParser.getSampleSummary())
    val activitySummary: StateFlow<ActivitySummary> = _activitySummary.asStateFlow()

    private val _routeConfig = MutableStateFlow(RouteStyleConfig())
    val routeConfig: StateFlow<RouteStyleConfig> = _routeConfig.asStateFlow()

    private val _exportState = MutableStateFlow<ExportResult>(ExportResult.Idle)
    val exportState: StateFlow<ExportResult> = _exportState.asStateFlow()

    fun loadGpxFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val parsedSummary = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        GpxParser.parse(inputStream)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (parsedSummary != null) {
                _activitySummary.value = parsedSummary
            }
        }
    }

    fun updatePresetTheme(theme: PresetTheme) {
        _routeConfig.update { it.copy(presetTheme = theme) }
    }

    fun updateCustomLocation(location: String) {
        _routeConfig.update { it.copy(customLocation = location) }
    }

    fun updateCustomTitle(title: String) {
        _routeConfig.update { it.copy(customTitle = title) }
    }

    fun updateOverrideDistance(distanceKm: Double?) {
        _routeConfig.update { it.copy(overrideDistanceKm = distanceKm) }
    }

    fun updateOverrideDuration(duration: String?) {
        _routeConfig.update { it.copy(overrideDuration = duration) }
    }

    fun updateOverridePace(pace: String?) {
        _routeConfig.update { it.copy(overridePace = pace) }
    }

    fun updateOverrideCalories(calories: Int?) {
        _routeConfig.update { it.copy(overrideCalories = calories) }
    }

    fun updateOverrideHeartRate(hr: Int?) {
        _routeConfig.update { it.copy(overrideHeartRate = hr) }
    }

    fun updateOverrideElevation(ele: Int?) {
        _routeConfig.update { it.copy(overrideElevation = ele) }
    }

    fun updateRoutePosition(pos: RoutePosition) {
        _routeConfig.update { it.copy(routePosition = pos) }
    }

    fun updateStatsPosition(pos: StatsPosition) {
        _routeConfig.update { it.copy(statsPosition = pos) }
    }

    fun toggleShowTitle(show: Boolean) {
        _routeConfig.update { it.copy(showTitle = show) }
    }

    fun toggleShowCalories(show: Boolean) {
        _routeConfig.update { it.copy(showCalories = show) }
    }

    fun toggleShowHeartRate(show: Boolean) {
        _routeConfig.update { it.copy(showHeartRate = show) }
    }

    fun toggleShowElevation(show: Boolean) {
        _routeConfig.update { it.copy(showElevation = show) }
    }

    fun toggleShowLocationTag(show: Boolean) {
        _routeConfig.update { it.copy(showLocationTag = show) }
    }

    fun updateGlowTheme(theme: NeonGlowTheme) {
        _routeConfig.update { it.copy(glowTheme = theme) }
    }

    fun updateStrokeWidth(widthPx: Float) {
        _routeConfig.update { it.copy(strokeWidthPx = widthPx) }
    }

    fun updateGlowRadius(radiusPx: Float) {
        _routeConfig.update { it.copy(glowRadiusPx = radiusPx) }
    }

    fun toggleStatsOverlay(show: Boolean) {
        _routeConfig.update { it.copy(showStatsOverlay = show) }
    }

    fun toggleSplitsChart(show: Boolean) {
        _routeConfig.update { it.copy(showSplitsChart = show) }
    }

    fun toggleMiniRouteCard(show: Boolean) {
        _routeConfig.update { it.copy(showMiniRouteCard = show) }
    }

    fun updateAspectRatio(aspectRatio: AspectRatioOption) {
        _routeConfig.update { it.copy(aspectRatio = aspectRatio) }
    }

    fun exportPng(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportResult.Loading
            val summary = _activitySummary.value
            val config = _routeConfig.value

            val resultUri = withContext(Dispatchers.IO) {
                try {
                    val bitmap = PngExporter.createOverlayBitmap(summary, config)
                    PngExporter.saveBitmapToGallery(context, bitmap)
                } catch (e: Exception) {
                    null
                }
            }

            if (resultUri != null) {
                _exportState.value = ExportResult.Success(resultUri)
            } else {
                _exportState.value = ExportResult.Error("Failed to export PNG. Check storage permissions.")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportResult.Idle
    }
}
