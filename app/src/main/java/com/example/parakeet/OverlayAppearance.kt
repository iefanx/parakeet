package com.example.parakeet

import android.content.Context
import android.graphics.Color

data class OverlayAppearance(
    val sizeDp: Int,
    val opacityPercent: Int,
    val accentColor: Int,
    val style: Int,
    val alwaysShow: Boolean,
    val enabled: Boolean
)

object OverlayAppearanceStore {
    const val PREFS = "floating_overlay"
    const val X_KEY = "x"
    const val Y_KEY = "y"
    const val SIZE_KEY = "size_dp"
    const val OPACITY_KEY = "opacity_percent"
    const val COLOR_KEY = "accent_color"
    const val STYLE_KEY = "style"
    const val ALWAYS_SHOW_KEY = "always_show"
    const val ENABLED_KEY = "enabled"
    const val IN_APP_EDITOR_FOCUSED_KEY = "in_app_editor_focused"
    const val APPEARANCE_TAB_ACTIVE_KEY = "appearance_tab_active"

    const val STYLE_WAVE = 0
    const val STYLE_MIC = 1
    const val STYLE_DOT = 2
    const val STYLE_CUSTOM_IMAGE = 3

    const val DEFAULT_X = 20
    const val DEFAULT_SIZE_DP = 56
    const val DEFAULT_OPACITY_PERCENT = 92
    const val MIN_SIZE_DP = 40
    const val MAX_SIZE_DP = 80
    const val MIN_OPACITY_PERCENT = 20
    const val MAX_OPACITY_PERCENT = 100

    val colorChoices = listOf(
        Color.parseColor("#7C8CFF"),
        Color.parseColor("#06B6D4"),
        Color.parseColor("#22C55E"),
        Color.parseColor("#A855F7"),
        Color.parseColor("#F97316"),
        Color.parseColor("#F4F4F5")
    )

    fun customImageFile(context: Context): java.io.File =
        java.io.File(context.filesDir, "custom_button_image.png")

    fun load(context: Context): OverlayAppearance {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return OverlayAppearance(
            sizeDp = prefs.getInt(SIZE_KEY, DEFAULT_SIZE_DP).coerceIn(MIN_SIZE_DP, MAX_SIZE_DP),
            opacityPercent = prefs.getInt(OPACITY_KEY, DEFAULT_OPACITY_PERCENT)
                .coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT),
            accentColor = prefs.getInt(COLOR_KEY, colorChoices.first()),
            style = prefs.getInt(STYLE_KEY, STYLE_WAVE).coerceIn(STYLE_WAVE, STYLE_CUSTOM_IMAGE),
            alwaysShow = prefs.getBoolean(ALWAYS_SHOW_KEY, false),
            enabled = prefs.getBoolean(ENABLED_KEY, true)
        )
    }

    fun save(context: Context, appearance: OverlayAppearance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(SIZE_KEY, appearance.sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP))
            .putInt(OPACITY_KEY, appearance.opacityPercent.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT))
            .putInt(COLOR_KEY, appearance.accentColor)
            .putInt(STYLE_KEY, appearance.style.coerceIn(STYLE_WAVE, STYLE_CUSTOM_IMAGE))
            .putBoolean(ALWAYS_SHOW_KEY, appearance.alwaysShow)
            .putBoolean(ENABLED_KEY, appearance.enabled)
            .apply()
    }

    fun reset(context: Context): OverlayAppearance {
        val appearance = OverlayAppearance(
            sizeDp = DEFAULT_SIZE_DP,
            opacityPercent = DEFAULT_OPACITY_PERCENT,
            accentColor = colorChoices.first(),
            style = STYLE_WAVE,
            alwaysShow = false,
            enabled = true
        )
        save(context, appearance)
        return appearance
    }

    fun resetPosition(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(X_KEY)
            .remove(Y_KEY)
            .apply()
    }

    fun isInAppEditorFocused(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(IN_APP_EDITOR_FOCUSED_KEY, false)

    fun styleLabel(style: Int): String {
        return when (style) {
            STYLE_MIC -> "Mic"
            STYLE_DOT -> "Dot"
            STYLE_CUSTOM_IMAGE -> "Custom"
            else -> "Wave"
        }
    }

    fun alphaFraction(opacityPercent: Int): Float {
        return opacityPercent.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT) / 100f
    }
}
