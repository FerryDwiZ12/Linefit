package com.example.linefit.renderer

import androidx.compose.ui.graphics.Color
import com.example.linefit.ui.theme.CoralRed
import com.example.linefit.ui.theme.ElectricBlue
import com.example.linefit.ui.theme.NeonRouteGreen
import com.example.linefit.ui.theme.PureWhite
import com.example.linefit.ui.theme.VoltLime

enum class AspectRatioOption(val label: String, val ratioWidth: Int, val ratioHeight: Int) {
    STORY_9_16("9:16 (Story/Reels)", 9, 16),
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

data class RouteStyleConfig(
    val glowTheme: NeonGlowTheme = NeonGlowTheme.NEON_GREEN,
    val strokeWidthPx: Float = 12f,
    val glowRadiusPx: Float = 28f,
    val showStatsOverlay: Boolean = true,
    val showSplitsChart: Boolean = true,
    val showMiniRouteCard: Boolean = true,
    val aspectRatio: AspectRatioOption = AspectRatioOption.STORY_9_16
)
