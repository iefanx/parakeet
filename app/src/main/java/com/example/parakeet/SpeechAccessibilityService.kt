package com.example.parakeet

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpeechAccessibilityService : AccessibilityService() {
    private val TAG = "SpeechAccessibility"
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var micDrawable: MicDrawable? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var focusedNode: AccessibilityNodeInfo? = null
    
    // Hardened with SupervisorJob to prevent child coroutine crashes from killing the service scope
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var timerJob: Job? = null
    private var secondsElapsed = 0
    
    private var transcribeJob: Job? = null
    private var visibilityJob: Job? = null
    private var isRecording = false
    private var lastFocusedEditableTime = 0L
    private var overlayPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var quickSettingsOverlay: QuickSettingsOverlay? = null

    // ─── Haptic feedback helper ───────────────────────────────────────
    private fun performHaptic(type: HapticType = HapticType.TICK) {
        val view = floatingView
        if (view != null) {
            val constant = when (type) {
                HapticType.TICK -> if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY
                HapticType.HEAVY -> HapticFeedbackConstants.LONG_PRESS
                HapticType.CONFIRM -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
                HapticType.REJECT -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            return
        }
        // Fallback: no view is attached, use Vibrator directly
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= 26) {
                val ms = when (type) {
                    HapticType.TICK -> 8L
                    HapticType.HEAVY -> 25L
                    HapticType.CONFIRM -> 15L
                    HapticType.REJECT -> 12L
                }
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10L)
            }
        } catch (_: Exception) { }
    }

    private enum class HapticType { TICK, HEAVY, CONFIRM, REJECT }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in overlayAppearanceKeys) {
                applyFloatingButtonAppearance()
                if (key == OverlayAppearanceStore.ALWAYS_SHOW_KEY || key == OverlayAppearanceStore.APPEARANCE_TAB_ACTIVE_KEY || key == OverlayAppearanceStore.ENABLED_KEY) {
                    scheduleOverlayVisibilityCheck(0)
                }
            }
            if (key == OverlayAppearanceStore.IN_APP_EDITOR_FOCUSED_KEY) {
                if (OverlayAppearanceStore.isInAppEditorFocused(this) && isParakeetForeground()) {
                    // Compose fields are not consistently exposed as FOCUS_INPUT nodes to an
                    // accessibility service. Treat the app's explicit focus bridge as the
                    // source of truth while Paraflow itself is foreground.
                    showFloatingButton()
                } else {
                    updateFocusedNode(null)
                    hideFloatingButton()
                }
            }
        }
        getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(overlayPrefsListener)
        // Pre-initialize SpeechManager to start model loading
        SpeechManager.getRecognizer(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventNode = try { event.source } catch (_: Exception) { null }
        val eventNodeIsEditable = try { eventNode?.isEditable == true } catch (_: Exception) { false }
        if (eventNodeIsEditable) {
            lastFocusedEditableTime = System.currentTimeMillis()
            updateFocusedNode(eventNode)
            try { eventNode?.recycle() } catch (_: Exception) { }
            showFloatingButton()
            return
        }
        try { eventNode?.recycle() } catch (_: Exception) { }

        val packageName = event.packageName?.toString()
        if (packageName == this.packageName) {
            // The quick-settings card is itself an accessibility overlay. Keep it alive while
            // its rows are being tapped; the card handles its own outside-tap dismissal.
            if (quickSettingsOverlay?.isVisible == true) return
            // A tap on the accessibility overlay can be reported back as a click in our
            // own window. Once recording or processing has started, that event must not
            // be interpreted as a tap outside the editor and remove the active control.
            if (isRecording || micDrawable?.isLoading == true) return
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                updateFocusedNode(null)
                hideFloatingButton()
                return
            }
            scheduleOverlayVisibilityCheck()
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> scheduleOverlayVisibilityCheck()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    private fun isParakeetForeground(): Boolean {
        val activeWindows = windows ?: return rootInActiveWindow?.packageName?.toString() == packageName
        for (window in activeWindows) {
            if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                val rootPackage = try {
                    window.root?.packageName?.toString()
                } catch (e: Exception) {
                    null
                }
                if (rootPackage == packageName) return true
            }
        }
        return false
    }

    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val roots = mutableListOf<AccessibilityNodeInfo?>()
        roots.add(rootInActiveWindow)
        windows?.forEach { window ->
            if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                roots.add(try {
                    window.root
                } catch (e: Exception) {
                    null
                })
            }
        }

        for (root in roots) {
            val focused = try {
                root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            } catch (e: Exception) {
                null
            } ?: continue

            val usable = try {
                focused.isEditable
            } catch (e: Exception) {
                false
            }

            if (usable) return focused

            try {
                focused.recycle()
            } catch (e: Exception) {
                // Ignore stale accessibility nodes.
            }
        }

        return null
    }

    private fun scheduleOverlayVisibilityCheck(delayMs: Long = 80) {
        if (isRecording || micDrawable?.isLoading == true || quickSettingsOverlay?.isVisible == true) return

        visibilityJob?.cancel()
        visibilityJob = serviceScope.launch {
            delay(delayMs)
            refreshOverlayVisibility()
        }
    }

    private fun refreshOverlayVisibility() {
        if (quickSettingsOverlay?.isVisible == true) return
        val isInsideApp = isParakeetForeground()
        val appearance = OverlayAppearanceStore.load(this)
        if (!isInsideApp && !appearance.enabled) {
            hideFloatingButton()
            return
        }

        val focusedEditable = findFocusedEditableNode()
        val appearanceTabActive = getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
            .getBoolean(OverlayAppearanceStore.APPEARANCE_TAB_ACTIVE_KEY, false) && isInsideApp

        if (focusedEditable != null) {
            lastFocusedEditableTime = System.currentTimeMillis()
            Log.d(TAG, "Focused editable field found in ${focusedEditable.packageName}")
            updateFocusedNode(focusedEditable)
            try {
                focusedEditable.recycle()
            } catch (e: Exception) {
                // Ignore stale accessibility nodes.
            }
            showFloatingButton()
        } else if (appearanceTabActive) {
            showFloatingButton()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastFocusedEditableTime < 1500) {
                showFloatingButton()
            } else {
                Log.d(TAG, "No focused editable field")
                val retainedOwnEditor = try {
                    focusedNode?.packageName?.toString() == packageName && isInsideApp
                } catch (_: Exception) { false }
                if (retainedOwnEditor) {
                    showFloatingButton()
                } else {
                    updateFocusedNode(null)
                    if (appearance.alwaysShow) showFloatingButton() else hideFloatingButton()
                }
            }
        }
    }

    private fun updateFocusedNode(node: AccessibilityNodeInfo?) {
        try {
            focusedNode?.recycle()
        } catch (e: Exception) {
            // Ignore stale accessibility nodes.
        }
        focusedNode = node?.let { AccessibilityNodeInfo.obtain(it) }
    }

    private fun showFloatingButton() {
        val isInsideApp = isParakeetForeground()
        val appearance = OverlayAppearanceStore.load(this)
        if (!isInsideApp && !appearance.enabled) {
            hideFloatingButton()
            return
        }

        visibilityJob?.cancel()
        visibilityJob = null
        if (floatingView != null) return

        val ctx = this
        val sizePx = dpToPx(appearance.sizeDp)
        val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
        
        val drawable = MicDrawable(this).apply { applyAppearance(appearance) }
        micDrawable = drawable
        
        val imageView = ImageView(ctx).apply {
            setImageDrawable(drawable)
        }
        floatingView = imageView

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            alpha = OverlayAppearanceStore.alphaFraction(appearance.opacityPercent)
            val savedPosition = getFloatingButtonPosition(sizePx)
            x = savedPosition.first
            y = savedPosition.second
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchTime = 0L

        floatingView?.setOnTouchListener { view, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchTime = System.currentTimeMillis()
                    micDrawable?.pressProgress = 1f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val position = clampFloatingButtonPosition(
                        initialX - (event.rawX - initialTouchX).toInt(),
                        initialY + (event.rawY - initialTouchY).toInt(),
                        params.width,
                        params.height
                    )
                    params.x = position.first
                    params.y = position.second
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    micDrawable?.pressProgress = 0f
                    val diffX = event.rawX - initialTouchX
                    val diffY = event.rawY - initialTouchY
                    val duration = System.currentTimeMillis() - touchTime
                    if (Math.abs(diffX) < touchSlop && Math.abs(diffY) < touchSlop) {
                        if (duration > 600) {
                            view.performLongClick()
                        } else {
                            view.performClick()
                        }
                    } else {
                        saveFloatingButtonPosition(params.x, params.y)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    micDrawable?.pressProgress = 0f
                    saveFloatingButtonPosition(params.x, params.y)
                    true
                }
                else -> false
            }
        }

        floatingView?.setOnClickListener {
            performHaptic(HapticType.TICK)
            handleMicClick()
        }

        floatingView?.setOnLongClickListener {
            performHaptic(HapticType.HEAVY)
            showQuickSettings()
            true
        }

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view to window manager", e)
            floatingView = null
            micDrawable = null
            layoutParams = null
        }
    }

    private fun showQuickSettings() {
        if (isRecording || micDrawable?.isLoading == true) return
        val view = floatingView ?: return
        val params = layoutParams ?: return
        if (quickSettingsOverlay?.isVisible == true) return

        quickSettingsOverlay = QuickSettingsOverlay(this, object : QuickSettingsOverlay.Callbacks {
            override fun onGrammarLevelSelected(level: Int) {
                PostProcessingStore.saveGrammarLevel(this@SpeechAccessibilityService, level)
                if (level > 0) {
                    val language = when (level) {
                        1 -> com.google.mlkit.nl.translate.TranslateLanguage.FRENCH
                        2 -> com.google.mlkit.nl.translate.TranslateLanguage.SPANISH
                        else -> com.google.mlkit.nl.translate.TranslateLanguage.GERMAN
                    }
                    serviceScope.launch {
                        try {
                            OnDeviceTranslationManager.download(language)
                        } catch (e: Exception) {
                            Log.w(TAG, "Grammar resources will download on first use", e)
                        }
                    }
                }
                val label = when (level) {
                    1 -> "Low"
                    2 -> "Medium"
                    3 -> "Intense"
                    else -> "Off"
                }
                Toast.makeText(this@SpeechAccessibilityService, "Grammar correction: $label", Toast.LENGTH_SHORT).show()
            }

            override fun onTranslationSelected(language: TranslationLanguageOption?) {
                if (language == null) {
                    PostProcessingStore.saveTranslation(
                        this@SpeechAccessibilityService,
                        enabled = false,
                        target = PostProcessingStore.targetLanguage(this@SpeechAccessibilityService)
                    )
                    Toast.makeText(this@SpeechAccessibilityService, "Translation off", Toast.LENGTH_SHORT).show()
                    return
                }

                // Persist the choice immediately. The existing translation pipeline will retry
                // the offline model download if the background warm-up cannot run right now.
                PostProcessingStore.saveTranslation(
                    this@SpeechAccessibilityService,
                    enabled = true,
                    target = language.tag
                )
                serviceScope.launch {
                    try {
                        OnDeviceTranslationManager.download(language.tag)
                    } catch (e: Exception) {
                        Log.w(TAG, "Translation model will download on first use", e)
                    }
                }
                Toast.makeText(this@SpeechAccessibilityService, "Translation: ${language.name}", Toast.LENGTH_SHORT).show()
            }

            override fun onSettingsClicked() {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open Android Settings", e)
                }
            }
        }).also {
            it.show(windowManager ?: return, params, view.width, view.height)
        }
    }

    private fun getFloatingButtonPosition(buttonWidth: Int): Pair<Int, Int> {
        val prefs = getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
        val defaultY = (resources.displayMetrics.heightPixels * 0.6).toInt()
        val x = prefs.getInt(OverlayAppearanceStore.X_KEY, OverlayAppearanceStore.DEFAULT_X)
        val y = prefs.getInt(OverlayAppearanceStore.Y_KEY, defaultY)
        return clampFloatingButtonPosition(x, y, buttonWidth, buttonWidth)
    }

    private fun saveFloatingButtonPosition(x: Int, y: Int) {
        getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(OverlayAppearanceStore.X_KEY, x)
            .putInt(OverlayAppearanceStore.Y_KEY, y)
            .apply()
    }

    private fun clampFloatingButtonPosition(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int> {
        val displayMetrics = resources.displayMetrics
        val maxX = (displayMetrics.widthPixels - width).coerceAtLeast(0)
        val maxY = (displayMetrics.heightPixels - height).coerceAtLeast(0)
        return Pair(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    private fun applyFloatingButtonAppearance() {
        val params = layoutParams ?: return
        val view = floatingView ?: return
        val appearance = OverlayAppearanceStore.load(this)
        val sizePx = dpToPx(appearance.sizeDp)
        params.width = sizePx
        params.height = sizePx
        val savedPosition = getFloatingButtonPosition(sizePx)
        params.x = savedPosition.first
        params.y = savedPosition.second
        params.alpha = OverlayAppearanceStore.alphaFraction(appearance.opacityPercent)
        micDrawable?.applyAppearance(appearance)
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating floating button appearance", e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun handleMicClick() {
        if (focusedNode == null) {
            Toast.makeText(this, "Tap a text field first, then use Paraflow.", Toast.LENGTH_SHORT).show()
            return
        }
        val recognizer = SpeechManager.getRecognizer(this)
        val recorder = SpeechManager.getRecorder()

        if (!recognizer.isModelLoaded) {
            Toast.makeText(this, "Speech model is still loading offline. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isRecording) {
            val success = recorder.startRecording(serviceScope)
            if (success) {
                isRecording = true
                micDrawable?.isRecording = true
                performHaptic(HapticType.CONFIRM)
                
                // Start elapsed timer loop
                secondsElapsed = 0
                micDrawable?.elapsedSeconds = 0
                timerJob?.cancel()
                timerJob = serviceScope.launch {
                    while (isRecording) {
                        delay(1000)
                        secondsElapsed++
                        micDrawable?.elapsedSeconds = secondsElapsed
                    }
                }
                Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()
            } else {
                performHaptic(HapticType.REJECT)
                Toast.makeText(this, "Failed to start recording. Check microphone permission.", Toast.LENGTH_SHORT).show()
            }
        } else {
            isRecording = false
            timerJob?.cancel()
            timerJob = null
            performHaptic(HapticType.CONFIRM)
            
            micDrawable?.isRecording = false
            micDrawable?.isLoading = true
            Toast.makeText(this, "Transcribing...", Toast.LENGTH_SHORT).show()
            
            transcribeJob?.cancel()
            transcribeJob = serviceScope.launch {
                val samples = recorder.stopRecording()
                val transcription = recognizer.transcribe(samples)
                micDrawable?.isLoading = false
                if (transcription.isNotEmpty() && !transcription.startsWith("Error transcribing") && transcription != "Model not loaded") {
                    val text = try {
                        val corrected = OnDeviceGrammarManager.correctIfEnabled(this@SpeechAccessibilityService, transcription)
                        OnDeviceTranslationManager.translateIfEnabled(this@SpeechAccessibilityService, corrected)
                    } catch (e: Exception) {
                        Log.e(TAG, "Optional post-processing failed; using transcription", e)
                        Toast.makeText(this@SpeechAccessibilityService, "Post-processing unavailable; inserted English text", Toast.LENGTH_SHORT).show()
                        transcription
                    }
                    // Save translation to persistent history
                    HistoryManager.saveEntry(this@SpeechAccessibilityService, text)
                    // Insert translation at cursor
                    pasteText(text)
                } else if (transcription.startsWith("Error transcribing")) {
                    Toast.makeText(this@SpeechAccessibilityService, "Error transcribing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pasteText(text: String) {
        val node = focusedNode ?: return
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val originalClip = clipboard.primaryClip

            // Format text with smart spacing (industry standard helper)
            val formattedText = formatTextForInsertion(text)

            // Set new transcribed text to clipboard
            val clip = ClipData.newPlainText("transcription", formattedText)
            clipboard.setPrimaryClip(clip)

            // ACTION_PASTE is the least invasive route and preserves rich content in most apps.
            // A few editors deliberately disable it, so fall back to ACTION_SET_TEXT there.
            val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (!pasted) {
                val existing = node.text?.toString().orEmpty()
                val start = node.textSelectionStart.coerceIn(0, existing.length)
                val end = node.textSelectionEnd.coerceIn(start, existing.length)
                val replacement = existing.replaceRange(start, end, formattedText)
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replacement)
                    }
                )
            }

            // Restore original clipboard after a small delay
            serviceScope.launch {
                delay(200)
                try {
                    if (originalClip != null) {
                        clipboard.setPrimaryClip(originalClip)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboard.clearPrimaryClip()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore restore errors if clipboard became locked or altered
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to paste text via clipboard", e)
        }
    }

    private fun formatTextForInsertion(text: String): String {
        val node = focusedNode ?: return text
        try {
            val content = node.text?.toString()
            if (content.isNullOrEmpty()) {
                return text
            }

            val selStart = node.textSelectionStart
            val charBefore = if (selStart in 1..content.length) {
                content[selStart - 1]
            } else if (selStart == 0) {
                // Cursor at the start -> no space
                null
            } else {
                // Fallback: check the last character of the text box if selection index is unavailable
                content.lastOrNull()
            }

            if (charBefore != null && charBefore != ' ' && charBefore != '\n') {
                return " $text"
            }
        } catch (e: Exception) {
            // Fallback check
            if (!text.startsWith(".") && !text.startsWith(",") && !text.startsWith("?")) {
                return " $text"
            }
        }
        return text
    }

    private fun hideFloatingButton() {
        visibilityJob?.cancel()
        visibilityJob = null
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // Ignore
            }
            floatingView = null
            micDrawable = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        quickSettingsOverlay?.dismiss()
        quickSettingsOverlay = null
        timerJob?.cancel()
        transcribeJob?.cancel()
        visibilityJob?.cancel()
        overlayPrefsListener?.let {
            getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(it)
        }
        overlayPrefsListener = null
        serviceScope.coroutineContext.cancelChildren() // Clean up any active coroutines
        updateFocusedNode(null)
        hideFloatingButton()
    }

    companion object {
        private val overlayAppearanceKeys = setOf(
            OverlayAppearanceStore.SIZE_KEY,
            OverlayAppearanceStore.OPACITY_KEY,
            OverlayAppearanceStore.COLOR_KEY,
            OverlayAppearanceStore.STYLE_KEY,
            OverlayAppearanceStore.ALWAYS_SHOW_KEY,
            OverlayAppearanceStore.ENABLED_KEY,
            OverlayAppearanceStore.X_KEY,
            OverlayAppearanceStore.Y_KEY,
            OverlayAppearanceStore.APPEARANCE_TAB_ACTIVE_KEY
        )

        fun setInAppEditorFocused(context: Context, focused: Boolean) {
            context.getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(OverlayAppearanceStore.IN_APP_EDITOR_FOCUSED_KEY, focused).apply()
        }

        fun isEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, SpeechAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledService = ComponentName.unflattenFromString(componentNameString)
                if (enabledService != null && enabledService == expectedComponentName) {
                    return true
                }
            }
            return false
        }
    }
}
