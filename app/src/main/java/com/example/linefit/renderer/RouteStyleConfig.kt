package com.example.linefit.renderer

import androidx.compose.ui.graphics.Color
import com.example.linefit.ui.theme.CoralRed
import com.example.linefit.ui.theme.ElectricBlue
import com.example.linefit.ui.theme.NeonRouteGreen
import com.example.linefit.ui.theme.PureWhite
import com.example.linefit.ui.theme.VoltLime

enum class AspectRatioOption(val label: String, val ratioWidth: Int, val ratioHeight: Int) {
    STORY_9_16("9:16 (Story)", 9, 16),
    SQUARE_1_1("1:1 (Square)", 1, 1),
    FEED_4_5("4:5 (Feed)", 4, 5)
}

enum class NeonGlowTheme(val displayName: String, val glowColor: Color, val coreColor: Color) {
    NEON_GREEN("Cyber Green", NeonRouteGreen, VoltLime),
    VOLT_LIME("Volt Lime", VoltLime, Color(0xFFE6FFE6)),
    ELECTRIC_BLUE("Electric Blue", ElectricBlue, Color(0xFFE0F7FA)),
    CORAL_RED("Coral Red", CoralRed, Color(0xFFFFEBEE)),
    PURE_WHITE("Pure White", PureWhite, PureWhite)
}

enum class PresetTheme(val displayName: String, val category: String) {
    THE_VOLT_PULSE("The Volt Pulse", "Athletic Studio"),
    MINIMALIST_GLASS_PILL("Minimalist Glass Pill", "Modern Compact"),
    PACE_HEATMAP_RIBBON("Pace Heatmap Ribbon", "Performance Data"),
    TELEMETRY_HUD("Telemetry HUD", "Futuristic HUD"),
    MAGAZINE_EDITORIAL("Magazine Editorial", "Typography"),
    SPLIT_ANALYTICAL("Split Analytical", "Performance Data"),
    TRAIL_HORIZON("Trail Horizon", "Outdoor 3D"),
    VIEWFINDER_FRAME("Viewfinder Frame", "Camera Overlay"),
    DUAL_RING_ACTIVITY("Dual Ring Activity", "Activity Gauges"),
    BRUTALIST_TICKET("Brutalist Ticket", "Brutalist Event"),
    DOODLE_NOTEBOOK("Doodle Notebook", "Hand Drawn Sketch"),
    CYBERPUNK_GRID("Cyberpunk Grid", "80s Retrowave"),
    CHALKBOARD_SKETCH("Chalkboard Sketch", "Textured Chalk"),
    RETRO_GAMING_PIXEL("Retro Gaming Pixel", "8-Bit Arcade"),
    MINIMAL_TOPOGRAPHY("Minimal Topography", "Outdoor Contour"),
    GOLDEN_CHAMPION("Golden Champion", "Luxury Trophy"),
    STEALTH_CARBON("Stealth Carbon", "Carbon Weave"),
    STRAVA_SUNSET("Strava Sunset", "Sunset Orange"),
    NEON_CYBERDREAM("Neon Cyberdream", "Vaporwave Neon"),
    VINTAGE_POSTCARD("Vintage Postcard", "Classic Travel")
}

enum class RoutePosition(val label: String) {
    CENTER("Center"),
    TOP_HEADER("Top Header"),
    RIGHT_SIDEBAR("Right Sidebar"),
    BACKGROUND_FULL("Background Full")
}

enum class StatsPosition(val label: String) {
    BOTTOM_BAR("Bottom Bar"),
    TOP_HEADER("Top Header"),
    OVERLAY_FLOATING("Overlay Floating")
}

data class RouteStyleConfig(
    val presetTheme: PresetTheme = PresetTheme.THE_VOLT_PULSE,
    val customLocation: String = "📍 Tambakboyo",
    val customTitle: String = "Morning Walk",
    val glowTheme: NeonGlowTheme = NeonGlowTheme.NEON_GREEN,
    val strokeWidthPx: Float = 12f,
    val glowRadiusPx: Float = 28f,
    val routePosition: RoutePosition = RoutePosition.CENTER,
    val statsPosition: StatsPosition = StatsPosition.BOTTOM_BAR,
    
    // Optional Manual Overrides
    val overrideDistanceKm: Double? = null,
    val overrideDuration: String? = null,
    val overridePace: String? = null,
    val overrideCalories: Int? = null,
    val overrideHeartRate: Int? = null,
    val overrideElevation: Int? = null,

    // Component Visibility Toggles
    val showTitle: Boolean = true,
    val showDistance: Boolean = true,
    val showTime: Boolean = true,
    val showPace: Boolean = true,
    val showCalories: Boolean = true,
    val showHeartRate: Boolean = true,
    val showElevation: Boolean = true,
    val showLocationTag: Boolean = true,
    val showStatsOverlay: Boolean = true,
    val showSplitsChart: Boolean = true,
    val showMiniRouteCard: Boolean = true,
    val aspectRatio: AspectRatioOption = AspectRatioOption.STORY_9_16
)
