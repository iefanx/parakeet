package com.example.parakeet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

/**
 * The long-press surface for the floating dictation control.
 *
 * This is intentionally a regular Android overlay rather than an Activity. That keeps the
 * interaction in-place over whichever app the user is currently writing in and avoids pulling
 * them out of context just to change a quick setting.
 */
class QuickSettingsOverlay(
    private val context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onGrammarLevelSelected(level: Int)
        fun onTranslationSelected(language: TranslationLanguageOption?)
        fun onSettingsClicked()
    }

    private enum class Screen { MAIN, GRAMMAR, TRANSLATION }

    // Dark theme palette – tuned for readability on dark overlay
    private val accent = android.graphics.Color.rgb(137, 149, 255)
    private val background = android.graphics.Color.rgb(20, 20, 28)
    private val elevated = android.graphics.Color.rgb(32, 32, 42)
    private val cardSurface = android.graphics.Color.rgb(26, 26, 36)
    private val primaryText = android.graphics.Color.rgb(240, 240, 245)
    private val secondaryText = android.graphics.Color.rgb(170, 174, 188)
    private val mutedText = android.graphics.Color.rgb(110, 114, 128)
    private val divider = android.graphics.Color.rgb(40, 40, 52)
    private val dp = context.resources.displayMetrics.density

    private var windowManager: WindowManager? = null
    private var root: FrameLayout? = null
    private var menu: LinearLayout? = null
    private var anchorParams: WindowManager.LayoutParams? = null
    private var anchorWidth = 0
    private var anchorHeight = 0
    private var screen = Screen.MAIN

    val isVisible: Boolean
        get() = root != null

    fun show(
        windowManager: WindowManager,
        anchorParams: WindowManager.LayoutParams,
        anchorWidth: Int,
        anchorHeight: Int
    ) {
        if (root != null) return

        this.windowManager = windowManager
        this.anchorParams = anchorParams
        this.anchorWidth = anchorWidth
        this.anchorHeight = anchorHeight
        screen = Screen.MAIN

        val overlay = object : FrameLayout(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_UP) dismiss()
                return true
            }
        }.apply {
            setBackgroundColor(android.graphics.Color.argb(80, 0, 0, 0))
            isClickable = true
        }
        root = overlay
        render()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(overlay, params)
            overlay.post { positionMenu() }
        } catch (_: Exception) {
            root = null
            menu = null
            this.windowManager = null
        }
    }

    fun dismiss() {
        val overlay = root ?: return
        root = null
        menu = null
        try {
            windowManager?.removeView(overlay)
        } catch (_: Exception) {
            // The service may already be tearing down its window token.
        }
        windowManager = null
    }

    // ─── Haptic helpers ───────────────────────────────────────────────

    private fun tickHaptic(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_TAP
            else HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    // ─── Layout ───────────────────────────────────────────────────────

    private fun render() {
        val overlay = root ?: return
        val nextMenu = surface()
        menu = nextMenu
        overlay.removeAllViews()
        overlay.addView(
            nextMenu,
            FrameLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        nextMenu.post { positionMenu() }
    }

    private fun positionMenu() {
        val overlay = root ?: return
        val currentMenu = menu ?: return
        if (currentMenu.width == 0 || currentMenu.height == 0) {
            currentMenu.measure(
                View.MeasureSpec.makeMeasureSpec(dp(220), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(overlay.height, View.MeasureSpec.AT_MOST)
            )
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val anchor = anchorParams ?: return
        val buttonLeft = screenWidth - anchor.x - anchor.width
        val menuWidth = currentMenu.measuredWidth.coerceAtLeast(dp(220))
        val menuHeight = currentMenu.measuredHeight
        val gap = dp(8)

        val left = if (buttonLeft >= menuWidth + gap) {
            buttonLeft - menuWidth - gap
        } else {
            buttonLeft + anchorWidth + gap
        }.coerceIn(dp(8), (screenWidth - menuWidth - dp(8)).coerceAtLeast(dp(8)))

        val centeredTop = anchor.y + (anchorHeight - menuHeight) / 2
        val top = centeredTop.coerceIn(
            dp(12),
            (screenHeight - menuHeight - dp(12)).coerceAtLeast(dp(12))
        )

        val params = currentMenu.layoutParams as FrameLayout.LayoutParams
        params.width = menuWidth
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.leftMargin = left
        params.topMargin = top
        currentMenu.layoutParams = params
    }

    private fun surface(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = rounded(this@QuickSettingsOverlay.background, 18)
            elevation = dp(14).toFloat()
            isClickable = true
            setOnClickListener { /* Keep touches inside the card. */ }
            clipToOutline = true

            when (screen) {
                Screen.MAIN -> buildMain(this)
                Screen.GRAMMAR -> buildGrammar(this)
                Screen.TRANSLATION -> buildTranslation(this)
            }
        }
    }

    // ─── Main ─────────────────────────────────────────────────────────

    private fun buildMain(parent: LinearLayout) {
        addCompactHeader(parent, "Quick settings", onBack = null)
        addThinDivider(parent)
        addCompactRow(
            parent,
            icon = IconType.GRAMMAR,
            title = "Grammar",
            value = grammarLabel(PostProcessingStore.grammarLevel(context)),
            onClick = {
                screen = Screen.GRAMMAR
                render()
            }
        )
        addCompactRow(
            parent,
            icon = IconType.TRANSLATION,
            title = "Translation",
            value = translationLabel(),
            onClick = {
                screen = Screen.TRANSLATION
                render()
            }
        )
        addThinDivider(parent)
        addCompactRow(
            parent,
            icon = IconType.SETTINGS,
            title = "Settings",
            value = "",
            showChevron = false,
            onClick = {
                dismiss()
                callbacks.onSettingsClicked()
            }
        )
    }

    // ─── Grammar ──────────────────────────────────────────────────────

    private fun buildGrammar(parent: LinearLayout) {
        addCompactHeader(parent, "Grammar", onBack = {
            screen = Screen.MAIN
            render()
        })
        addThinDivider(parent)
        val options = listOf("Off" to 0, "Low" to 1, "Medium" to 2, "Intense" to 3)
        options.forEach { (label, level) ->
            addCompactOption(
                parent,
                label = label,
                selected = PostProcessingStore.grammarLevel(context) == level,
                onClick = {
                    callbacks.onGrammarLevelSelected(level)
                    render()
                }
            )
        }
    }

    // ─── Translation ──────────────────────────────────────────────────

    private fun buildTranslation(parent: LinearLayout) {
        addCompactHeader(parent, "Translation", onBack = {
            screen = Screen.MAIN
            render()
        })
        addThinDivider(parent)

        val selected = PostProcessingStore.targetLanguage(context)
        val translationEnabled = PostProcessingStore.translationEnabled(context)
        val options = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        addCompactOption(
            options,
            label = "None",
            selected = !translationEnabled,
            onClick = {
                callbacks.onTranslationSelected(null)
                render()
            }
        )
        PostProcessingStore.languages.forEach { language ->
            addCompactOption(
                options,
                label = language.name,
                selected = language.tag == selected && translationEnabled,
                onClick = {
                    callbacks.onTranslationSelected(language)
                    render()
                }
            )
        }
        parent.addView(MaxHeightScrollView(context, dp(400)).apply {
            isFillViewport = true
            addView(options, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    // ─── Components ───────────────────────────────────────────────────

    private fun addCompactHeader(
        parent: LinearLayout,
        title: String,
        onBack: (() -> Unit)?
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(4), dp(6))
        }

        if (onBack != null) {
            val backBtn = FrameLayout(context).apply {
                background = pressableBackground(10)
                isClickable = true
                setOnClickListener { v ->
                    tickHaptic(v)
                    onBack()
                }
                addView(
                    QuickSettingsIconView(context, IconType.BACK, secondaryText),
                    FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER)
                )
            }
            row.addView(backBtn, LinearLayout.LayoutParams(dp(28), dp(28)))
            addSpacer(row, 4)
        }

        row.addView(
            text(title, 13, primaryText, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val closeBtn = FrameLayout(context).apply {
            background = pressableBackground(10)
            isClickable = true
            setOnClickListener { v ->
                tickHaptic(v)
                dismiss()
            }
            addView(
                QuickSettingsIconView(context, IconType.CLOSE, mutedText),
                FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER)
            )
        }
        row.addView(closeBtn, LinearLayout.LayoutParams(dp(26), dp(26)))
        parent.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun addCompactRow(
        parent: LinearLayout,
        icon: IconType,
        title: String,
        value: String,
        showChevron: Boolean = true,
        onClick: () -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(40)
            background = pressableBackground()
            setOnClickListener { v ->
                tickHaptic(v)
                onClick()
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        // Small icon badge
        val badge = FrameLayout(context).apply {
            background = rounded(elevated, 10)
            addView(
                QuickSettingsIconView(context, icon, accent),
                FrameLayout.LayoutParams(dp(15), dp(15), Gravity.CENTER)
            )
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(30), dp(30)))
        addSpacer(row, 8)

        // Title
        row.addView(
            text(title, 12, primaryText, false),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        // Value label
        if (value.isNotEmpty()) {
            row.addView(text(value, 11, accent, false).apply {
                setPadding(0, 0, dp(2), 0)
            })
        }

        // Chevron
        if (showChevron) {
            row.addView(
                QuickSettingsIconView(context, IconType.CHEVRON, mutedText),
                LinearLayout.LayoutParams(dp(10), dp(10))
            )
        }

        parent.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun addCompactOption(
        parent: LinearLayout,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(38)
            background = if (selected) rounded(accent.copy(alpha = 0.10f), 10) else pressableBackground(10)
            setOnClickListener { v ->
                tickHaptic(v)
                onClick()
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }

        // Radio indicator
        val indicator = QuickSettingsIconView(
            context,
            if (selected) IconType.CHECK else IconType.DOT,
            if (selected) accent else mutedText
        )
        row.addView(indicator, LinearLayout.LayoutParams(dp(16), dp(16)))
        addSpacer(row, 8)

        row.addView(
            text(label, 12, if (selected) primaryText else secondaryText, selected),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        parent.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun addThinDivider(parent: LinearLayout) {
        parent.addView(View(context).apply { setBackgroundColor(divider) }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(1)
        ).apply {
            topMargin = dp(3)
            bottomMargin = dp(3)
            leftMargin = dp(8)
            rightMargin = dp(8)
        })
    }

    private fun text(value: String, sizeSp: Int, color: Int, bold: Boolean): TextView {
        return TextView(context).apply {
            text = value
            textSize = sizeSp.toFloat()
            setTextColor(color)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (bold) typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
    }

    private fun addSpacer(parent: LinearLayout, width: Int) {
        parent.addView(View(context), LinearLayout.LayoutParams(dp(width), 1))
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun pressableBackground(radius: Int = 10): Drawable {
        val normal = rounded(android.graphics.Color.TRANSPARENT, radius)
        val pressed = rounded(android.graphics.Color.rgb(40, 40, 52), radius)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun grammarLabel(level: Int): String = when (level) {
        1 -> "Low"
        2 -> "Medium"
        3 -> "Intense"
        else -> "Off"
    }

    private fun translationLabel(): String {
        if (!PostProcessingStore.translationEnabled(context)) return "Off"
        return PostProcessingStore.languages.firstOrNull {
            it.tag == PostProcessingStore.targetLanguage(context)
        }?.name ?: "Off"
    }

    private fun Int.copy(alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return android.graphics.Color.argb(a, android.graphics.Color.red(this), android.graphics.Color.green(this), android.graphics.Color.blue(this))
    }

    private fun dp(value: Int): Int = (value * dp).toInt()

    private enum class IconType { WAVE, GRAMMAR, TRANSLATION, SETTINGS, BACK, CLOSE, CHECK, DOT, CHEVRON }

    private class MaxHeightScrollView(context: Context, private val maxHeight: Int) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if (measuredHeight > maxHeight) {
                setMeasuredDimension(measuredWidth, maxHeight)
            }
        }
    }

    private class QuickSettingsIconView(
        context: Context,
        private val icon: IconType,
        private val color: Int
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 1.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val s = minOf(width, height).toFloat()
            when (icon) {
                IconType.WAVE -> {
                    val heights = floatArrayOf(.18f, .34f, .52f, .34f, .18f)
                    heights.forEachIndexed { index, h ->
                        val x = cx + (index - 2) * s * .14f
                        canvas.drawLine(x, cy - s * h, x, cy + s * h, paint)
                    }
                }
                IconType.GRAMMAR -> {
                    canvas.drawLine(s * .15f, s * .25f, s * .85f, s * .25f, paint)
                    canvas.drawLine(s * .15f, s * .50f, s * .65f, s * .50f, paint)
                    canvas.drawLine(s * .15f, s * .75f, s * .45f, s * .75f, paint)
                    val path = Path().apply {
                        moveTo(s * .62f, s * .70f)
                        lineTo(s * .72f, s * .80f)
                        lineTo(s * .90f, s * .58f)
                    }
                    canvas.drawPath(path, paint)
                }
                IconType.TRANSLATION -> {
                    canvas.drawCircle(cx, cy, s * .38f, paint)
                    canvas.drawOval(cx - s * .18f, cy - s * .38f, cx + s * .18f, cy + s * .38f, paint)
                    canvas.drawLine(cx - s * .38f, cy, cx + s * .38f, cy, paint)
                }
                IconType.SETTINGS -> {
                    canvas.drawCircle(cx, cy, s * .14f, paint)
                    for (i in 0 until 8) {
                        val angle = Math.PI * i / 4.0
                        val x1 = cx + cos(angle).toFloat() * s * .26f
                        val y1 = cy + sin(angle).toFloat() * s * .26f
                        val x2 = cx + cos(angle).toFloat() * s * .42f
                        val y2 = cy + sin(angle).toFloat() * s * .42f
                        canvas.drawLine(x1, y1, x2, y2, paint)
                    }
                }
                IconType.BACK -> {
                    canvas.drawLine(s * .72f, cy, s * .28f, cy, paint)
                    canvas.drawLine(s * .28f, cy, s * .48f, s * .28f, paint)
                    canvas.drawLine(s * .28f, cy, s * .48f, s * .72f, paint)
                }
                IconType.CLOSE -> {
                    canvas.drawLine(s * .28f, s * .28f, s * .72f, s * .72f, paint)
                    canvas.drawLine(s * .72f, s * .28f, s * .28f, s * .72f, paint)
                }
                IconType.CHECK -> {
                    canvas.drawLine(s * .18f, s * .52f, s * .42f, s * .76f, paint)
                    canvas.drawLine(s * .42f, s * .76f, s * .84f, s * .26f, paint)
                }
                IconType.DOT -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, s * .12f, paint)
                    paint.style = Paint.Style.STROKE
                }
                IconType.CHEVRON -> {
                    canvas.drawLine(s * .32f, s * .22f, s * .68f, s * .50f, paint)
                    canvas.drawLine(s * .68f, s * .50f, s * .32f, s * .78f, paint)
                }
            }
        }
    }
}
