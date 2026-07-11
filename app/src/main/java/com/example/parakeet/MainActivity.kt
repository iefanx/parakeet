package com.example.parakeet

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Color Palette ──────────────────────────────────────────────────
private val BgPrimary    = Color(0xFF000000)
private val BgCard       = Color(0xFF0B0B0E)
private val BgElevated   = Color(0xFF15151A)
private val Accent       = Color(0xFF8995FF)
private val TextPrimary  = Color(0xFFF5F5F7)
private val TextSecondary= Color(0xFFB4B6C0)
private val TextMuted    = Color(0xFF777B87)
private val GreenActive  = Color(0xFF53C78B)
private val RedRecord    = Color(0xFFE85D69)
private val CyanSpinner  = Color(0xFF79D8E8)
private val DividerColor = Color(0xFF25252C)

enum class AppState {
    LOADING_MODEL, READY, RECORDING, TRANSCRIBING, ERROR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SpeechManager.getRecognizer(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgPrimary) {
                    MainScreen()
                }
            }
        }
    }
}

// ── Logo Icon ──────────────────────────────────────────────────────
@Composable
fun LogoIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val s = (size * 0.90f) / 24f
        val sw = size * 0.058f
        fun mx(x: Float) = cx + (x - 12f) * s
        fun my(y: Float) = cy + (y - 12f) * s
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(mx(6f), my(11f)), androidx.compose.ui.geometry.Offset(mx(6f), my(13f)), sw, cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(mx(9f), my(9f)), androidx.compose.ui.geometry.Offset(mx(9f), my(15f)), sw, cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(mx(12f), my(6f)), androidx.compose.ui.geometry.Offset(mx(12f), my(18f)), sw, cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(mx(15f), my(9f)), androidx.compose.ui.geometry.Offset(mx(15f), my(15f)), sw, cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(mx(18f), my(11f)), androidx.compose.ui.geometry.Offset(mx(18f), my(13f)), sw, cap)
        drawCircle(color, 9f * s, androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
    }
}

// ── Root Navigation ────────────────────────────────────────────────
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("parakeet_prefs", Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarding_completed", false)) }
    var targetStep by remember { mutableStateOf(0) }

    if (onboarded) {
        DashboardScreen(onResetOnboarding = { step ->
            targetStep = step
            prefs.edit().putBoolean("onboarding_completed", false).apply()
            onboarded = false
        })
    } else {
        OnboardingScreen(initialStep = targetStep, onCompleted = {
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            onboarded = true
        })
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  ONBOARDING
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun OnboardingScreen(initialStep: Int = 0, onCompleted: () -> Unit) {
    val context = LocalContext.current
    val privacyPrefs = remember { context.getSharedPreferences("parakeet_privacy", Context.MODE_PRIVATE) }
    var step by remember(initialStep) { mutableStateOf(initialStep) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    var hasMic by remember { mutableStateOf(false) }
    var hasA11y by remember { mutableStateOf(false) }
    var hasBattery by remember { mutableStateOf(false) }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }

    // Track which steps have already been auto-advanced to avoid repeat toasts
    val autoAdvanced = remember { mutableSetOf<Int>() }

    fun refresh() {
        hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasA11y = SpeechAccessibilityService.isEnabled(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        hasBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Auto-advance: when a step's permission is granted, show toast and move to next step
    fun tryAutoAdvance() {
        val granted = when (step) {
            2 -> hasA11y
            3 -> hasBattery
            else -> false
        }
        if (granted && step !in autoAdvanced && step in 2..3) {
            autoAdvanced.add(step)
            val msg = when (step) {
                2 -> "Accessibility service active ✓"
                3 -> "Background access granted ✓"
                else -> "Permission granted ✓"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            step++
        }
    }

    // Re-check on every lifecycle resume + auto-advance
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                refresh()
                tryAutoAdvance()
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Re-check when the user navigates to any step (catches returning from settings)
    LaunchedEffect(step) {
        refresh()
        tryAutoAdvance()
        // Also poll briefly in case ON_RESUME was missed (e.g. overlay settings)
        delay(600)
        refresh()
        tryAutoAdvance()
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMic = granted
        if (granted) {
            Toast.makeText(context, "Microphone access granted ✓", Toast.LENGTH_SHORT).show()
            step = 2
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Top: brand + step pills ──
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    LogoIcon(Modifier.size(18.dp), Accent)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Paraflow", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Private, offline dictation", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("STEP ${step + 1} OF 5", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                color = TextMuted, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                repeat(5) { i ->
                    val done = when (i) {
                        0 -> true  // intro always "done"
                        1 -> hasMic
                        2 -> hasA11y
                        3 -> hasBattery
                        else -> false
                    }
                    val color = when {
                        i == step -> Accent
                        done && i < step -> GreenActive
                        else -> Color(0xFF27272A)
                    }
                    Box(
                        Modifier.padding(horizontal = 3.dp)
                            .size(width = if (i == step) 20.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        // ── Center: step content ──
        val scrollState = rememberScrollState()
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    0 -> StepIntro()
                    1 -> StepPermission(
                        title = "Microphone access",
                        desc = "Required for speech recognition. Audio is processed entirely on your device — nothing is sent to the cloud.",
                        isGranted = hasMic,
                        grantLabel = "Allow microphone",
                        grantedLabel = "Microphone ready",
                        onGrant = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        instructions = listOf("A system dialog will appear", "Tap \"Allow\" to grant microphone access")
                    )
                    2 -> StepAccessibility(
                        isGranted = hasA11y,
                        onConsentAndGrant = {
                            privacyPrefs.edit().putBoolean("accessibility_consent_v1", true).apply()
                            // Try to deep-link to installed services on Android 11+
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                // Add fragment arguments to scroll closer to our service
                                intent.putExtra(":settings:fragment_args_key", "${context.packageName}/.SpeechAccessibilityService")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }
                    )
                    3 -> StepPermission(
                        title = "Background access",
                        desc = "Keeps the dictation service available reliably while you move between apps.",
                        isGranted = hasBattery,
                        grantLabel = "Allow",
                        grantedLabel = "Background access ready",
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            })
                        },
                        instructions = listOf("A system dialog will appear", "Tap \"Allow\" to prevent battery restrictions")
                    )
                    4 -> StepGuide()
                }
            }
        }

        // ── Bottom: navigation ──
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                Text("Back", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { step-- }.padding(12.dp))
            } else {
                Spacer(Modifier.width(60.dp))
            }

            Button(
                onClick = { if (step < 4) step++ else onCompleted() },
                enabled = step != 1 || hasMic,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    disabledContainerColor = Color(0xFF27272A)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    if (step == 4) "Start using Paraflow" else "Continue",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        Text(
            "Privacy policy",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.clickable { showPrivacyPolicy = true }.padding(8.dp)
        )
    }
}

@Composable
private fun StepIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 28.dp)
    ) {
        val defaultAppearance = remember {
            OverlayAppearance(
                sizeDp = OverlayAppearanceStore.DEFAULT_SIZE_DP,
                opacityPercent = OverlayAppearanceStore.DEFAULT_OPACITY_PERCENT,
                accentColor = OverlayAppearanceStore.colorChoices.first(),
                style = OverlayAppearanceStore.STYLE_WAVE,
                alwaysShow = false,
                enabled = true
            )
        }

        Box(
            Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            OverlayButtonFace(
                appearance = defaultAppearance,
                appState = AppState.READY,
                elapsed = 0,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Welcome to Paraflow",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your System-Wide Writing Assistant",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Accent,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Paraflow integrates directly into your OS, acting as an overlay voice dictation layer available anywhere you type.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MinimalistFeatureRow("Voice Dictation", "Dictate into any text box with offline accuracy.")
            MinimalistFeatureRow("Grammar Correction", "Automatically formats and corrects speech on-device.")
            MinimalistFeatureRow("Language Translation", "Optionally translate dictation offline in real-time.")
            MinimalistFeatureRow("Privacy First", "All processing runs locally. Your audio never leaves your device.")
        }
    }
}

@Composable
private fun MinimalistFeatureRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Accent)
        )
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(1.dp))
            Text(desc, fontSize = 11.5.sp, color = TextMuted, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun StepPermission(
    title: String, desc: String, isGranted: Boolean,
    grantLabel: String, grantedLabel: String, onGrant: () -> Unit,
    instructions: List<String> = emptyList()
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(if (isGranted) GreenActive.copy(alpha = 0.1f) else BgElevated)
                .border(1.dp, if (isGranted) GreenActive.copy(alpha = 0.3f) else DividerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Text("✓", fontSize = 24.sp, color = GreenActive, fontWeight = FontWeight.Bold)
            } else {
                LogoIcon(Modifier.size(28.dp), Accent)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(desc, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center,
            lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))

        // Numbered instruction steps
        if (!isGranted && instructions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier.fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("HOW TO", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                instructions.forEachIndexed { i, text ->
                    Row(
                        Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (isGranted) {
            Text(grantedLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = GreenActive)
        } else {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(grantLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
        }
    }
}

// Dedicated accessibility step with detailed instructions
@Composable
private fun StepAccessibility(isGranted: Boolean, onConsentAndGrant: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(if (isGranted) GreenActive.copy(alpha = 0.1f) else BgElevated)
                .border(1.dp, if (isGranted) GreenActive.copy(alpha = 0.3f) else DividerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Text("✓", fontSize = 24.sp, color = GreenActive, fontWeight = FontWeight.Bold)
            } else {
                LogoIcon(Modifier.size(28.dp), Accent)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Allow dictation in other apps", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Paraflow uses Android Accessibility only to show the dictation button beside a text field and insert the words you ask it to transcribe. Voice, text, and history stay on this device.",
            fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center,
            lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))

        if (!isGranted) {
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(12.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Accent.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                        Text("1", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Tap Open settings below", fontSize = 12.sp, color = TextSecondary)
                }
                Row(
                    Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Accent.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                        Text("2", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.ui.text.AnnotatedString.Builder().apply {
                        append("Open ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                        append("Installed apps")
                        pop()
                        append(" or ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                        append("Downloaded services")
                        pop()
                    }.let {
                        Text(it.toAnnotatedString(), fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Row(
                    Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Accent.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                        Text("3", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Select Paraflow and turn it on", fontSize = 12.sp, color = TextSecondary)
                }

                // Guide Card
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Accent.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Setup Tip",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "On newer Samsung, Pixel, and OnePlus devices, look for the 'Installed apps' or 'Downloaded apps' submenu to find the Paraflow service toggle.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        if (isGranted) {
            Text("Service Active", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = GreenActive)
        } else {
            Button(
                onClick = onConsentAndGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun StepGuide() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(GreenActive.copy(alpha = 0.1f))
                .border(1.dp, GreenActive.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 24.sp, color = GreenActive, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("You're all set", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Here's how to use Paraflow", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(BgCard, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            listOf(
                "Open any app and tap a text field",
                "The floating button will appear on screen",
                "Tap to start recording your voice",
                "Tap again to stop — text appears instantly"
            ).forEachIndexed { i, text ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Privacy policy", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "Paraflow records audio only while you choose to dictate. Speech recognition happens on your device. " +
                    "The Accessibility service observes the focused editable field to show the dictation control and inserts only the transcription you request. " +
                    "It may inspect nearby text to add a space correctly.\n\n" +
                    "No audio, transcriptions, or typed text are sent off-device or shared. Transcription history is stored locally on your device and can be deleted from Usage. " +
                    "Paraflow has no account, advertising, analytics, cloud sync, or third-party data sharing.",
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = Accent) }
        }
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DASHBOARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun DashboardScreen(onResetOnboarding: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recognizer = SpeechManager.getRecognizer(context)
    val recorder = SpeechManager.getRecorder()
    val modelDownloadState by ModelDownloadManager.state.collectAsState()

    var state by remember { mutableStateOf(AppState.LOADING_MODEL) }
    var statusText by remember { mutableStateOf("Loading model…") }
    var notepad by remember { mutableStateOf(TextFieldValue("")) }
    var tab by remember { mutableStateOf("playground") }
    var showHistory by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<HistoryEntry>()) }
    var overlayAppearance by remember { mutableStateOf(OverlayAppearanceStore.load(context)) }
    var elapsed by remember { mutableStateOf(0) }
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Permissions
    var hasA11y by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(false) }
    var hasBattery by remember { mutableStateOf(false) }

    fun refresh() {
        hasA11y = SpeechAccessibilityService.isEnabled(context)
        hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        hasBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
        history = HistoryManager.getHistory(context)
        overlayAppearance = OverlayAppearanceStore.load(context)
    }

    fun saveOverlayAppearance(appearance: OverlayAppearance) {
        overlayAppearance = appearance
        OverlayAppearanceStore.save(context, appearance)
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) {
        refresh()
        state = AppState.LOADING_MODEL
        val ok = recognizer.loadModel()
        state = if (ok) AppState.READY else AppState.ERROR
        statusText = if (ok) "Ready" else "Model load failed"
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMic = granted
            if (granted) {
                if (recorder.startRecording(scope)) {
                    state = AppState.RECORDING
                    elapsed = 0
                    timerJob?.cancel()
                    timerJob = scope.launch { while (state == AppState.RECORDING) { delay(1000); elapsed++ } }
                }
            }
        }

    fun insertAtCursor(text: String) {
        val src = notepad.text
        val sel = notepad.selection
        val prefix = if (sel.start > 0 && src.getOrNull(sel.start - 1).let { it != null && it != ' ' && it != '\n' }) " " else ""
        val insert = "$prefix$text"
        val newText = StringBuilder(src).replace(sel.min, sel.max, insert).toString()
        val newCursor = sel.min + insert.length
        notepad = TextFieldValue(newText, TextRange(newCursor))
    }

    fun handleRecord() {
        when (state) {
            AppState.READY -> {
                if (hasMic) {
                    if (recorder.startRecording(scope)) {
                        state = AppState.RECORDING
                        elapsed = 0
                        timerJob?.cancel()
                        timerJob = scope.launch { while (state == AppState.RECORDING) { delay(1000); elapsed++ } }
                    }
                } else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            AppState.RECORDING -> {
                timerJob?.cancel(); timerJob = null
                state = AppState.TRANSCRIBING; statusText = "Transcribing…"
                scope.launch {
                    val samples = recorder.stopRecording()
                    val text = recognizer.transcribe(samples)
                    if (text.isBlank()) {
                        state = AppState.READY; statusText = "No speech detected"
                    } else if (text.startsWith("Error") || text == "Model not loaded") {
                        state = AppState.ERROR; statusText = text
                    } else {
                        val processedText = try {
                            val corrected = OnDeviceGrammarManager.correctIfEnabled(context, text)
                            OnDeviceTranslationManager.translateIfEnabled(context, corrected)
                        } catch (e: Exception) {
                            text
                        }
                        insertAtCursor(processedText)
                        HistoryManager.saveEntry(context, processedText)
                        history = HistoryManager.getHistory(context)
                        state = AppState.READY; statusText = "Ready"
                    }
                }
            }
            else -> {}
        }
    }

    fun retryModelLoad() {
        if (state == AppState.LOADING_MODEL) return
        state = AppState.LOADING_MODEL
        statusText = "Loading model…"
        scope.launch {
            val ok = recognizer.loadModel()
            state = if (ok) AppState.READY else AppState.ERROR
            statusText = if (ok) "Ready" else "Model load failed"
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .imePadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = .13f))
                        .border(1.dp, Accent.copy(alpha = .24f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { LogoIcon(Modifier.size(30.dp), Accent) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Paraflow", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Private, on-device dictation", fontSize = 11.sp, color = TextMuted)
                }
            }
            CompactActionPill(if (showHistory || showEditor) "‹  Back" else "Setup") {
                when {
                    showHistory -> showHistory = false
                    showEditor -> showEditor = false
                    else -> onResetOnboarding(0)
                }
            }
        }

        // ── Tab bar ──
        if (!showHistory && !showEditor) {
            Row(
                Modifier.fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                TabItem("Home", tab == "playground", Modifier.weight(1f)) { tab = "playground" }
                TabItem("Appearance", tab == "appearance", Modifier.weight(1f)) { tab = "appearance" }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Content ──
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (showHistory) {
                HistoryPage(
                    entries = history,
                    context = context,
                    onHistoryChanged = { history = HistoryManager.getHistory(context) }
                )
            } else if (showEditor) {
                EditorPage(notepad = notepad, onNotepadChange = { notepad = it })
            } else if (tab == "playground") {
                PlaygroundTab(
                    notepad = notepad,
                    onNotepadChange = { notepad = it },
                    appState = state,
                    modelDownloadState = modelDownloadState,
                    hasA11y = hasA11y,
                    hasBattery = hasBattery,
                    entries = history,
                    onOpenHistory = { showHistory = true },
                    onOpenEditor = { showEditor = true },
                    onRetryModel = { retryModelLoad() },
                    onConfigA11y = {
                        onResetOnboarding(2)
                    },
                    onConfigBattery = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    },
                    appearance = overlayAppearance,
                    onAppearanceChange = { saveOverlayAppearance(it) }
                )
            } else {
                AppearanceTab(
                    appearance = overlayAppearance,
                    onAppearanceChange = { saveOverlayAppearance(it) },
                    onReset = { saveOverlayAppearance(OverlayAppearanceStore.reset(context)) },
                    onResetPosition = { OverlayAppearanceStore.resetPosition(context) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) BgElevated else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (active) TextPrimary else TextMuted)
    }
}

@Composable
private fun CompactActionPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(28.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (label.contains("Back")) BgElevated else Accent.copy(alpha = .09f))
            .border(1.dp, if (label.contains("Back")) DividerColor else Accent.copy(alpha = .28f), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, color = if (label.contains("Back")) TextSecondary else Accent,
            fontWeight = FontWeight.SemiBold, letterSpacing = .2.sp)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PLAYGROUND
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun PlaygroundTab(
    notepad: TextFieldValue, onNotepadChange: (TextFieldValue) -> Unit,
    appState: AppState,
    modelDownloadState: ModelDownloadState,
    hasA11y: Boolean, hasBattery: Boolean,
    entries: List<HistoryEntry>,
    onOpenHistory: () -> Unit,
    onOpenEditor: () -> Unit,
    onRetryModel: () -> Unit,
    onConfigA11y: () -> Unit, onConfigBattery: () -> Unit,
    appearance: OverlayAppearance,
    onAppearanceChange: (OverlayAppearance) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val outsideTap = remember { MutableInteractionSource() }

    Column(
        Modifier.fillMaxSize().clickable(
            interactionSource = outsideTap,
            indication = null
        ) { focusManager.clearFocus() }
    ) {
        // Scrollable content area for top modules (Activities, Perms, Smart Outputs)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val readyEverywhere = hasA11y && hasBattery
            val missingCount = listOf(hasA11y, hasBattery).count { !it }
            if (appState == AppState.LOADING_MODEL) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Accent.copy(alpha = .09f), RoundedCornerShape(16.dp))
                        .border(1.dp, Accent.copy(alpha = .24f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (modelDownloadState is ModelDownloadState.Downloading) "Downloading speech model"
                                else "Preparing speech model",
                                fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (modelDownloadState is ModelDownloadState.Downloading) {
                                    val percent = (modelDownloadState.progress * 100).toInt().coerceIn(0, 100)
                                    "$percent% · one-time download"
                                } else "Getting offline dictation ready…",
                                fontSize = 12.sp, color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (modelDownloadState is ModelDownloadState.Downloading) {
                        LinearProgressIndicator(
                            progress = { modelDownloadState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Accent,
                            trackColor = BgElevated
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Accent,
                            trackColor = BgElevated
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (readyEverywhere) {
                CompactUsageSummary(entries = entries, onOpenHistory = onOpenHistory)
                Spacer(Modifier.height(12.dp))
            } else {
                Column(
                    Modifier.fillMaxWidth()
                        .background(BgCard, RoundedCornerShape(16.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Accent.copy(alpha = .14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(missingCount.toString(), color = Accent, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text(
                                if (missingCount == 1) "One setup step remaining"
                                else "$missingCount setup steps remaining",
                                fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold
                            )
                            Text("Finish setup below to use Paraflow across apps.", fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Permissions banner (compact, only when needed)
            val missing = !hasA11y || !hasBattery
            if (missing) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(BgCard, RoundedCornerShape(12.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text("FINISH SETUP", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = TextMuted, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    if (!hasA11y) PermRow("Accessibility service", hasA11y, onConfigA11y)
                    if (!hasBattery) {
                        if (!hasA11y) Spacer(Modifier.height(8.dp))
                        PermRow("Background access", hasBattery, onConfigBattery)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (appState == AppState.ERROR) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(RedRecord.copy(alpha = .10f), RoundedCornerShape(12.dp))
                        .border(1.dp, RedRecord.copy(alpha = .28f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Speech model unavailable", fontSize = 13.sp, color = TextPrimary,
                            fontWeight = FontWeight.SemiBold)
                        Text("Try loading the offline model again.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text("Retry", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onRetryModel).padding(10.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // 7. ENABLE TRANSCRIBING LAYER MASTER SWITCH & ALWAYS SHOW SWITCH
            val isA11yGranted = hasA11y
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .clickable {
                        if (isA11yGranted) {
                            onAppearanceChange(appearance.copy(enabled = !appearance.enabled))
                        } else {
                            android.widget.Toast.makeText(context, "Please enable Accessibility Service first", android.widget.Toast.LENGTH_SHORT).show()
                            onConfigA11y()
                        }
                    }
                    .alpha(if (isA11yGranted) 1f else 0.5f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable transcribing layer", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isA11yGranted) "Integrate voice overlay globally" else "Requires Accessibility permission",
                        fontSize = 11.sp,
                        color = if (isA11yGranted) TextMuted else RedRecord
                    )
                }
                Switch(
                    checked = appearance.enabled && isA11yGranted,
                    onCheckedChange = {
                        if (isA11yGranted) {
                            onAppearanceChange(appearance.copy(enabled = it))
                        } else {
                            android.widget.Toast.makeText(context, "Please enable Accessibility Service first", android.widget.Toast.LENGTH_SHORT).show()
                            onConfigA11y()
                        }
                    },
                    enabled = isA11yGranted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgElevated,
                        uncheckedBorderColor = DividerColor
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(Modifier.height(16.dp))

            PostProcessingPanel()

            Spacer(Modifier.height(16.dp))

            val alwaysShowEnabled = isA11yGranted && appearance.enabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .then(
                        if (alwaysShowEnabled) {
                            Modifier.clickable { onAppearanceChange(appearance.copy(alwaysShow = !appearance.alwaysShow)) }
                        } else Modifier
                    )
                    .alpha(if (alwaysShowEnabled) 1f else 0.5f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Always show button", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("Keep button visible when not typing", fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = appearance.alwaysShow && alwaysShowEnabled,
                    onCheckedChange = { if (alwaysShowEnabled) onAppearanceChange(appearance.copy(alwaysShow = it)) },
                    enabled = alwaysShowEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgElevated,
                        uncheckedBorderColor = DividerColor
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Single-line input bar at the bottom with keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(BgCard)
                .border(1.dp, DividerColor, RoundedCornerShape(27.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = notepad,
                onValueChange = onNotepadChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        SpeechAccessibilityService.setInAppEditorFocused(context, it.isFocused)
                    },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                placeholder = {
                    Text("Type or dictate here…", color = TextMuted, fontSize = 14.sp)
                }
            )

            ExpandEditorButton(
                onClick = onOpenEditor
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExpandEditorButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(BgElevated)
            .border(1.dp, DividerColor, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(14.dp)) {
            val c = Accent
            val w = 1.8.dp.toPx()
            val width = size.width
            val height = size.height

            // Top-right arrow
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.45f, height * 0.55f),
                end = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.05f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.05f),
                end = androidx.compose.ui.geometry.Offset(width * 0.60f, height * 0.05f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.05f),
                end = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.40f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Bottom-left arrow
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.55f, height * 0.45f),
                end = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.95f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.95f),
                end = androidx.compose.ui.geometry.Offset(width * 0.40f, height * 0.95f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = c,
                start = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.95f),
                end = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.60f),
                strokeWidth = w,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun EditorPage(notepad: TextFieldValue, onNotepadChange: (TextFieldValue) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Text("Editor", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Focus the page and use the floating Paraflow button to compose.", fontSize = 11.sp,
            color = TextMuted)
        Spacer(Modifier.height(12.dp))
        TextField(
            value = notepad,
            onValueChange = onNotepadChange,
            modifier = Modifier.fillMaxWidth().weight(1f).onFocusChanged {
                SpeechAccessibilityService.setInAppEditorFocused(context, it.isFocused)
            },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = BgCard,
                unfocusedContainerColor = BgCard,
                cursorColor = Accent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
            placeholder = { Text("Start writing…", color = TextMuted) }
        )
    }
}

@Composable
private fun PostProcessingPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var translationEnabled by remember { mutableStateOf(PostProcessingStore.translationEnabled(context)) }
    var targetLanguage by remember { mutableStateOf(PostProcessingStore.targetLanguage(context)) }
    var languageMenu by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val selectedName = PostProcessingStore.languages.firstOrNull { it.tag == targetLanguage }?.name ?: "Spanish"

    var grammarLevel by remember { mutableStateOf(PostProcessingStore.grammarLevel(context)) }
    var grammarDownloading by remember { mutableStateOf(false) }
    var grammarStatus by remember { mutableStateOf<String?>(null) }

    fun enableTranslation(target: String) {
        val targetName = PostProcessingStore.languages.firstOrNull { it.tag == target }?.name ?: "language"
        downloading = true
        status = "Downloading $targetName for offline use…"
        scope.launch {
            try {
                OnDeviceTranslationManager.download(target)
                targetLanguage = target
                translationEnabled = true
                PostProcessingStore.saveTranslation(context, true, target)
                status = null
            } catch (e: Exception) {
                translationEnabled = false
                PostProcessingStore.saveTranslation(context, false, target)
                status = "Download failed. Check Wi-Fi and retry."
            } finally {
                downloading = false
            }
        }
    }

    fun selectGrammarLevel(level: Int) {
        if (level == 0) {
            grammarLevel = 0
            PostProcessingStore.saveGrammarLevel(context, 0)
            grammarStatus = null
            return
        }

        val targetLang = when (level) {
            1 -> com.google.mlkit.nl.translate.TranslateLanguage.FRENCH
            2 -> com.google.mlkit.nl.translate.TranslateLanguage.SPANISH
            3 -> com.google.mlkit.nl.translate.TranslateLanguage.GERMAN
            else -> com.google.mlkit.nl.translate.TranslateLanguage.SPANISH
        }

        grammarDownloading = true
        grammarStatus = "Downloading offline grammar resources…"
        scope.launch {
            try {
                OnDeviceTranslationManager.download(targetLang)
                grammarLevel = level
                PostProcessingStore.saveGrammarLevel(context, level)
                grammarStatus = null
            } catch (e: Exception) {
                grammarStatus = "Download failed. Check network connection."
            } finally {
                grammarDownloading = false
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text("Smart output", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("Optional processing after offline transcription.", fontSize = 10.sp, color = TextMuted)
        Spacer(Modifier.height(11.dp))

        // Grammar Correction Options with Segmented Selector
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Grammar correction", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text("Auto-format and clean spelling", fontSize = 9.sp, color = TextMuted)
                }
                if (grammarDownloading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Accent, strokeWidth = 1.5.dp)
                }
            }
            Spacer(Modifier.height(10.dp))
            
            // Segmented picker controls
            val levels = listOf("Off", "Low", "Medium", "Intense")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                levels.forEachIndexed { index, name ->
                    val selected = grammarLevel == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Accent else Color.Transparent)
                            .clickable(enabled = !grammarDownloading) { selectGrammarLevel(index) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Higher levels perform more aggressive grammatical correction and rephrasing.",
                fontSize = 9.5.sp,
                color = TextMuted,
                lineHeight = 13.sp
            )
        }

        if (grammarStatus != null) {
            Spacer(Modifier.height(4.dp))
            Text(grammarStatus!!, fontSize = 9.sp, color = RedRecord)
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
        Spacer(Modifier.height(12.dp))

        // Translate Output Section
        Text("Translate output", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Text("Select None for English, or download an offline language pack.",
            fontSize = 9.sp, color = TextMuted)
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth()) {
            SelectorButton(
                label = if (downloading) "Downloading…" else if (translationEnabled) selectedName else "None (English)",
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!downloading) languageMenu = true
            }
            
            // Dark themed dropdown wrapper
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surface = BgCard,
                    onSurface = TextPrimary,
                    surfaceVariant = BgElevated
                )
            ) {
                DropdownMenu(
                    expanded = languageMenu,
                    onDismissRequest = { languageMenu = false },
                    modifier = Modifier
                        .background(BgCard)
                        .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("None (English)") },
                        onClick = {
                            languageMenu = false
                            translationEnabled = false
                            status = null
                            PostProcessingStore.saveTranslation(context, false, targetLanguage)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TextPrimary,
                            leadingIconColor = TextMuted,
                            trailingIconColor = TextMuted
                        )
                    )
                    PostProcessingStore.languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.name) },
                            onClick = {
                                languageMenu = false
                                targetLanguage = language.tag
                                translationEnabled = false
                                PostProcessingStore.saveTranslation(context, false, language.tag)
                                enableTranslation(language.tag)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextPrimary,
                                leadingIconColor = TextMuted,
                                trailingIconColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
        if (downloading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Accent,
                trackColor = BgElevated
            )
            Spacer(Modifier.height(6.dp))
            Text("One-time download. Translation stays on-device afterward.",
                fontSize = 9.sp, color = TextMuted)
        } else if (status != null) {
            Spacer(Modifier.height(7.dp))
            Text(status!!, fontSize = 9.sp, color = RedRecord)
        }
    }
}

@Composable
private fun SelectorButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(38.dp).clip(RoundedCornerShape(11.dp)).background(BgElevated)
            .border(1.dp, DividerColor, RoundedCornerShape(11.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        Text("⌄", fontSize = 14.sp, color = TextMuted)
    }
}

@Composable
private fun CompactUsageSummary(entries: List<HistoryEntry>, onOpenHistory: () -> Unit) {
    val stats = remember(entries) { HistoryManager.getUsageStats(entries) }
    Column(
        Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your activity", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            CompactActionPill("History  ›", onOpenHistory)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactStat("WORDS TODAY", stats.todayWords.toString(), Modifier.weight(1f))
            CompactStat("SESSIONS", stats.entries.toString(), Modifier.weight(1f))
            CompactStat("CHARACTERS", stats.characters.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(BgElevated, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 9.dp)) {
        Text(value, fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.SemiBold,
            letterSpacing = .5.sp, maxLines = 1)
    }
}

@Composable
private fun PermRow(title: String, granted: Boolean, onConfigure: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Normal)
        if (granted) {
            Text("Active", fontSize = 11.sp, color = GreenActive, fontWeight = FontWeight.Medium)
        } else {
            Text("Configure", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onConfigure).padding(4.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  RECORD BUTTON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun RecordButton(
    appState: AppState,
    elapsed: Int,
    appearance: OverlayAppearance,
    onClick: () -> Unit
) {
    val buttonSize = appearance.sizeDp.dp
    val touchSize = (appearance.sizeDp + 16).dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(touchSize)) {
        // Pulse ring when recording
        if (appState == AppState.RECORDING) {
            val pulse = rememberInfiniteTransition("p")
            val s by pulse.animateFloat(1f, 1.18f,
                infiniteRepeatable(tween(850), RepeatMode.Reverse), "ps")
            Box(Modifier.size(buttonSize).scale(s).clip(CircleShape).background(RedRecord.copy(alpha = 0.16f)))
        }

        Box(
            Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .alpha(OverlayAppearanceStore.alphaFraction(appearance.opacityPercent))
                .clickable(enabled = appState == AppState.READY || appState == AppState.RECORDING) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            OverlayButtonFace(appearance, appState, elapsed, Modifier.fillMaxSize())
        }

        if (appState == AppState.TRANSCRIBING) {
            val rot by rememberInfiniteTransition("sp").animateFloat(0f, 360f,
                infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), "r")
            Canvas(Modifier.size(buttonSize)) {
                drawArc(CyanSpinner, rot, 80f, false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        size.minDimension * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun OverlayButtonFace(
    appearance: OverlayAppearance,
    appState: AppState,
    elapsed: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        val customImageFile = remember { OverlayAppearanceStore.customImageFile(context) }
        val lastModified = customImageFile.lastModified()
        val customBitmap = remember(lastModified) {
            if (customImageFile.exists()) {
                try {
                    android.graphics.BitmapFactory.decodeFile(customImageFile.absolutePath)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        if (appearance.style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE && customBitmap != null && appState != AppState.RECORDING) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            // Accent color border overlay
            Canvas(Modifier.fillMaxSize()) {
                val size = this.size.minDimension
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(cx, cy)
                drawCircle(
                    Color(appearance.accentColor).copy(alpha = 0.82f),
                    size / 2f - size * 0.02f,
                    center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.04f)
                )
            }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val size = this.size.minDimension
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(cx, cy)
                val background = when (appState) {
                    AppState.RECORDING -> RedRecord
                    AppState.TRANSCRIBING -> BgElevated
                    else -> Color(0xFF1E1E24)
                }
                drawCircle(background, size / 2f, center)
                if (appState != AppState.RECORDING) {
                    drawCircle(
                        Color(appearance.accentColor).copy(alpha = 0.82f),
                        size / 2f - size * 0.025f,
                        center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.04f)
                    )
                }

                if (appState == AppState.READY || appState == AppState.ERROR) {
                    drawOverlayIcon(appearance, size, cx, cy)
                }
            }
        }

        when (appState) {
            AppState.LOADING_MODEL -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(appearance.accentColor),
                strokeWidth = 2.dp
            )
            AppState.TRANSCRIBING -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(appearance.accentColor),
                strokeWidth = 2.dp
            )
            AppState.RECORDING -> {
                val t = formatElapsed(elapsed)
                Text(
                    t,
                    color = Color.White,
                    fontSize = if (t.length > 3) 11.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {}
        }
    }
}

private fun formatElapsed(sec: Int): String {
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return when {
        h > 0 -> "${h}h${m}m"
        m > 0 -> "${m}m${s}s"
        else -> "${s}s"
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  APPEARANCE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun StyleIconPreview(style: Int, selected: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val baseColor = if (selected) Accent else TextSecondary
    
    Box(modifier.size(36.dp), contentAlignment = Alignment.Center) {
        if (style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE) {
            val customImageFile = remember { OverlayAppearanceStore.customImageFile(context) }
            val lastModified = customImageFile.lastModified()
            val customBitmap = remember(lastModified) {
                if (customImageFile.exists()) {
                    try {
                        android.graphics.BitmapFactory.decodeFile(customImageFile.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    val size = this.size.minDimension
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    drawRoundRect(
                        baseColor,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - size * 0.25f, cy - size * 0.20f),
                        size = androidx.compose.ui.geometry.Size(size * 0.50f, size * 0.40f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.06f, size * 0.06f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.08f)
                    )
                    drawCircle(baseColor, size * 0.06f, androidx.compose.ui.geometry.Offset(cx - size * 0.10f, cy - size * 0.05f))
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - size * 0.20f, cy + size * 0.18f)
                        lineTo(cx - size * 0.05f, cy - size * 0.05f)
                        lineTo(cx + size * 0.10f, cy + size * 0.10f)
                        lineTo(cx + size * 0.15f, cy + size * 0.02f)
                        lineTo(cx + size * 0.22f, cy + size * 0.18f)
                    }
                    drawPath(
                        path,
                        baseColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val size = this.size.minDimension
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(cx, cy)
                
                when (style) {
                    OverlayAppearanceStore.STYLE_MIC -> {
                        drawRoundRect(
                            baseColor,
                            topLeft = androidx.compose.ui.geometry.Offset(cx - size * 0.15f, cy - size * 0.30f),
                            size = androidx.compose.ui.geometry.Size(size * 0.30f, size * 0.45f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.12f, size * 0.12f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.08f)
                        )
                        drawLine(
                            baseColor,
                            androidx.compose.ui.geometry.Offset(cx, cy + size * 0.22f),
                            androidx.compose.ui.geometry.Offset(cx, cy + size * 0.45f),
                            size * 0.08f
                        )
                    }
                    OverlayAppearanceStore.STYLE_DOT -> {
                        drawCircle(baseColor, size * 0.15f, center)
                        drawCircle(
                            baseColor,
                            size * 0.35f,
                            center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.08f)
                        )
                    }
                    OverlayAppearanceStore.STYLE_WAVE -> {
                        val path = androidx.compose.ui.graphics.Path()
                        val points = 30
                        val width = size * 0.7f
                        val startX = cx - width / 2f
                        val amplitude = size * 0.2f
                        for (i in 0..points) {
                            val x = startX + (width / points) * i
                            val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat() * 1.5f
                            val y = cy + Math.sin(angle.toDouble()).toFloat() * amplitude
                            if (i == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path,
                            baseColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HueSpectrumSlider(
    currentColor: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(currentColor) {
        FloatArray(3).apply {
            android.graphics.Color.colorToHSV(currentColor, this)
        }
    }
    val hue = hsv[0]
    
    BoxWithConstraints(modifier.fillMaxWidth().height(26.dp)) {
        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
            val spectrumColors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(spectrumColors),
                size = this.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
        
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxPx = with(density) { maxWidth.toPx() }
        
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(currentColor) {
                    detectTapGestures(
                        onPress = { offset ->
                            val fraction = (offset.x / maxPx).coerceIn(0f, 1f)
                            val selectedHue = fraction * 360f
                            val newHsv = floatArrayOf(selectedHue, 1f, 1f)
                            onColorChange(android.graphics.Color.HSVToColor(newHsv))
                        }
                    )
                }
                .pointerInput(currentColor) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val currentFraction = hue / 360f
                        val newFraction = (currentFraction + dragAmount / maxPx).coerceIn(0f, 1f)
                        val selectedHue = newFraction * 360f
                        val newHsv = floatArrayOf(selectedHue, 1f, 1f)
                        onColorChange(android.graphics.Color.HSVToColor(newHsv))
                    }
                }
        ) {
            val thumbOffset = (this@BoxWithConstraints.maxWidth.value * (hue / 360f)).dp
            Box(
                Modifier
                    .offset(x = thumbOffset - 10.dp, y = 3.dp)
                    .size(20.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(Color.White, CircleShape)
                    .border(3.dp, Color(currentColor), CircleShape)
            )
        }
    }
}

@Composable
private fun AppearanceTab(
    appearance: OverlayAppearance,
    onAppearanceChange: (OverlayAppearance) -> Unit,
    onReset: () -> Unit,
    onResetPosition: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        context.getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(OverlayAppearanceStore.APPEARANCE_TAB_ACTIVE_KEY, true).apply()
        onDispose {
            context.getSharedPreferences(OverlayAppearanceStore.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(OverlayAppearanceStore.APPEARANCE_TAB_ACTIVE_KEY, false).apply()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val destination = OverlayAppearanceStore.customImageFile(context)
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onAppearanceChange(appearance.copy(style = OverlayAppearanceStore.STYLE_CUSTOM_IMAGE))
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to copy custom image", e)
                android.widget.Toast.makeText(context, "Failed to load image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scroll = rememberScrollState()
    val sliderColors = SliderDefaults.colors(
        thumbColor = Accent,
        activeTrackColor = Accent,
        inactiveTrackColor = BgElevated
    )

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 16.dp)) {
        Text("Floating button", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Make the dictation control feel at home on your screen.", fontSize = 12.sp,
            color = TextMuted, lineHeight = 17.sp)
        Spacer(Modifier.height(16.dp))

        // 2. BUTTON DESIGN STYLE SELECTOR WITH GRAPHICAL ICON PREVIEWS
        SettingPanel("Style", "") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        OverlayAppearanceStore.STYLE_WAVE,
                        OverlayAppearanceStore.STYLE_MIC,
                        OverlayAppearanceStore.STYLE_DOT,
                        OverlayAppearanceStore.STYLE_CUSTOM_IMAGE
                    ).forEach { style ->
                        val selected = appearance.style == style
                        val bg = if (selected) Accent.copy(alpha = 0.12f) else BgElevated
                        val border = if (selected) Accent else DividerColor
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE) {
                                        val hasImage = OverlayAppearanceStore.customImageFile(context).exists()
                                        if (!hasImage) {
                                            imagePickerLauncher.launch("image/*")
                                        } else {
                                            onAppearanceChange(appearance.copy(style = style))
                                        }
                                    } else {
                                        onAppearanceChange(appearance.copy(style = style))
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StyleIconPreview(style, selected)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE) "Custom" else OverlayAppearanceStore.styleLabel(style),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Accent else TextSecondary
                            )
                        }
                    }
                }
                
                if (appearance.style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE && OverlayAppearanceStore.customImageFile(context).exists()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SlimSecondaryButton("Choose different image…", Modifier.fillMaxWidth(0.8f)) {
                            imagePickerLauncher.launch("image/*")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. COLOR SELECTOR WITH SPECTRUM GRADIENT AND CIRCLE PRESETS
        SettingPanel("Accent Color", "") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlayAppearanceStore.colorChoices.forEach { color ->
                        val selected = color == appearance.accentColor
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) TextPrimary else DividerColor,
                                    shape = CircleShape
                                )
                                .clickable { onAppearanceChange(appearance.copy(accentColor = color)) }
                        )
                    }
                }

                Column {
                    Text("Custom Color Spectrum", fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    HueSpectrumSlider(
                        currentColor = appearance.accentColor,
                        onColorChange = { newColor ->
                            onAppearanceChange(appearance.copy(accentColor = newColor))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. COMBINED SIZE & OPACITY IN A SINGLE COMPACT CARD
        SettingPanel("Size & Opacity", "") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Button Size", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text("${appearance.sizeDp} dp", fontSize = 11.sp, color = TextMuted)
                    }
                    Slider(
                        value = appearance.sizeDp.toFloat(),
                        onValueChange = { onAppearanceChange(appearance.copy(sizeDp = it.toInt())) },
                        valueRange = OverlayAppearanceStore.MIN_SIZE_DP.toFloat()..OverlayAppearanceStore.MAX_SIZE_DP.toFloat(),
                        colors = sliderColors,
                        modifier = Modifier.height(28.dp)
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text("${appearance.opacityPercent}%", fontSize = 11.sp, color = TextMuted)
                    }
                    Slider(
                        value = appearance.opacityPercent.toFloat(),
                        onValueChange = { onAppearanceChange(appearance.copy(opacityPercent = it.toInt())) },
                        valueRange = OverlayAppearanceStore.MIN_OPACITY_PERCENT.toFloat()..OverlayAppearanceStore.MAX_OPACITY_PERCENT.toFloat(),
                        colors = sliderColors,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SlimSecondaryButton("Reset position", Modifier.weight(1f), onResetPosition)
            SlimSecondaryButton("Reset design", Modifier.weight(1f), onReset)
        }
    }
}

@Composable
private fun SettingPanel(title: String, value: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            if (value.isNotBlank()) {
                Text(value, fontSize = 12.sp, color = TextMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AppearanceSectionLabel(label: String) {
    Text(label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
}

@Composable
private fun SlimSecondaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(36.dp).clip(RoundedCornerShape(99.dp)).background(BgCard)
            .border(1.dp, DividerColor, RoundedCornerShape(99.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Accent.copy(alpha = 0.18f) else BgElevated
    val border = if (selected) Accent else DividerColor
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) TextPrimary else TextSecondary,
            fontWeight = FontWeight.SemiBold)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOverlayIcon(
    appearance: OverlayAppearance,
    size: Float,
    cx: Float,
    cy: Float
) {
    val iconColor = if (appearance.accentColor == OverlayAppearanceStore.colorChoices.last()) {
        Color(0xFF111827)
    } else {
        Color.White
    }
    when (appearance.style) {
        OverlayAppearanceStore.STYLE_MIC -> {
            drawRoundRect(
                iconColor,
                topLeft = androidx.compose.ui.geometry.Offset(cx - size * 0.12f, cy - size * 0.24f),
                size = androidx.compose.ui.geometry.Size(size * 0.24f, size * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.10f, size * 0.10f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.055f)
            )
            drawLine(
                iconColor,
                androidx.compose.ui.geometry.Offset(cx, cy + size * 0.18f),
                androidx.compose.ui.geometry.Offset(cx, cy + size * 0.40f),
                size * 0.055f
            )
        }
        OverlayAppearanceStore.STYLE_DOT -> {
            drawCircle(iconColor, size * 0.13f, androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(
                iconColor,
                size * 0.30f,
                androidx.compose.ui.geometry.Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(size * 0.05f)
            )
        }
        else -> {
            val scale = size * 0.70f / 24f
            fun mx(x: Float) = cx + (x - 12f) * scale
            fun my(y: Float) = cy + (y - 12f) * scale
            val cap = androidx.compose.ui.graphics.StrokeCap.Round
            drawLine(iconColor, androidx.compose.ui.geometry.Offset(mx(6f), my(11f)), androidx.compose.ui.geometry.Offset(mx(6f), my(13f)), size * 0.046f, cap)
            drawLine(iconColor, androidx.compose.ui.geometry.Offset(mx(9f), my(9f)), androidx.compose.ui.geometry.Offset(mx(9f), my(15f)), size * 0.046f, cap)
            drawLine(iconColor, androidx.compose.ui.geometry.Offset(mx(12f), my(6f)), androidx.compose.ui.geometry.Offset(mx(12f), my(18f)), size * 0.046f, cap)
            drawLine(iconColor, androidx.compose.ui.geometry.Offset(mx(15f), my(9f)), androidx.compose.ui.geometry.Offset(mx(15f), my(15f)), size * 0.046f, cap)
            drawLine(iconColor, androidx.compose.ui.geometry.Offset(mx(18f), my(11f)), androidx.compose.ui.geometry.Offset(mx(18f), my(13f)), size * 0.046f, cap)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  USAGE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HistoryPage(entries: List<HistoryEntry>, context: Context, onHistoryChanged: () -> Unit) {
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Transcription history", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary)
                Text("Long-press an entry for actions", fontSize = 11.sp, color = TextMuted)
            }
            if (entries.isNotEmpty()) {
                Text(
                    "Clear all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEF7A83),
                    modifier = Modifier.clickable {
                        HistoryManager.clearHistory(context)
                        onHistoryChanged()
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }.padding(4.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
                Text("No transcriptions yet", fontSize = 13.sp, color = TextMuted)
            }
        } else {
            entries.take(50).forEachIndexed { i, entry ->
                HistoryRow(
                    entry = entry,
                    context = context,
                    onDelete = {
                        HistoryManager.deleteEntry(context, entry.id)
                        onHistoryChanged()
                    }
                )
                if (i < entries.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(entry: HistoryEntry, context: Context, onDelete: () -> Unit) {
    var showActions by remember { mutableStateOf(false) }

    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            containerColor = BgCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Transcription", fontWeight = FontWeight.SemiBold) },
            text = { Text(entry.text, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 8) },
            confirmButton = {
                TextButton(onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Transcription", entry.text))
                    showActions = false
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy", color = Accent) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showActions = false }) { Text("Cancel", color = TextMuted) }
                    TextButton(onClick = {
                        showActions = false
                        onDelete()
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }) { Text("Delete", color = RedRecord) }
                }
            }
        )
    }

    Box {
        Column(
            Modifier.fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showActions = true }
                )
                .padding(vertical = 12.dp)
        ) {
            Text(entry.text, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp, maxLines = 4)
            Spacer(Modifier.height(4.dp))
            Text("${formatTime(entry.timestamp)} • ${HistoryManager.getUsageStats(listOf(entry)).words} words",
                fontSize = 10.sp, color = TextMuted)
        }
    }
}

private fun formatTime(ts: Long): String {
    val d = (System.currentTimeMillis() - ts) / 1000
    return when {
        d < 60 -> "Just now"
        d < 3600 -> "${d / 60}m ago"
        d < 86400 -> "${d / 3600}h ago"
        else -> java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(ts))
    }
}
