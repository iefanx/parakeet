package com.example.parakeet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock

class MicDrawable(
    private val context: Context,
    var accentColor: Int = OverlayAppearanceStore.colorChoices.first(),
    var opacityPercent: Int = OverlayAppearanceStore.DEFAULT_OPACITY_PERCENT,
    var style: Int = OverlayAppearanceStore.STYLE_WAVE
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    var isRecording = false
        set(value) {
            field = value
            invalidateSelf()
        }

    var isLoading = false
        set(value) {
            field = value
            invalidateSelf()
        }

    var elapsedSeconds = 0
        set(value) {
            field = value
            invalidateSelf()
        }

    var pressProgress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidateSelf()
        }

    private var lastTime = 0L
    private var rotationAngle = 0f
    private var breathePhase = 0f

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val size = Math.min(w, h)
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()

        val time = SystemClock.uptimeMillis()
        if (lastTime == 0L) lastTime = time
        val delta = time - lastTime
        lastTime = time

        breathePhase = (breathePhase + delta * 0.003f) % (Math.PI.toFloat() * 2f)

        // Update rotation for progress spinner if transcribing
        if (isLoading) {
            rotationAngle = (rotationAngle + delta * 0.2f) % 360f
            invalidateSelf()
        }

        // Draw background circle
        paint.style = Paint.Style.FILL
        if (isRecording) {
            paint.color = Color.parseColor("#E5484D")
            canvas.drawCircle(cx, cy, size / 2f, paint)
        } else if (isLoading) {
            paint.color = Color.parseColor("#2C2C2E")
            canvas.drawCircle(cx, cy, size / 2f, paint)
        } else if (style == OverlayAppearanceStore.STYLE_CUSTOM_IMAGE) {
            val customImageFile = OverlayAppearanceStore.customImageFile(context)
            var imageDrawn = false
            if (customImageFile.exists()) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(customImageFile.absolutePath)
                    if (bitmap != null) {
                        val path = android.graphics.Path().apply {
                            addCircle(cx, cy, size / 2f, android.graphics.Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)

                        val srcSize = Math.min(bitmap.width, bitmap.height)
                        val srcX = (bitmap.width - srcSize) / 2
                        val srcY = (bitmap.height - srcSize) / 2
                        val srcRect = Rect(srcX, srcY, srcX + srcSize, srcY + srcSize)
                        val destRect = RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)

                        canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                        canvas.restore()
                        imageDrawn = true
                    }
                } catch (e: Exception) {
                    // Ignore and fallback
                }
            }
            if (!imageDrawn) {
                paint.color = Color.parseColor("#1E1E24")
                canvas.drawCircle(cx, cy, size / 2f, paint)
            }
        } else {
            paint.color = Color.parseColor("#1E1E24")
            canvas.drawCircle(cx, cy, size / 2f, paint)
        }

        if (isRecording) {
            val pulse = (Math.sin(breathePhase.toDouble()).toFloat() + 1f) / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.045f
            paint.color = Color.argb((80 + pulse * 72).toInt(), 255, 255, 255)
            canvas.drawCircle(cx, cy, size * (0.40f + pulse * 0.035f), paint)
            invalidateSelf()
        }

        // Draw rotating spinner on border if transcribing
        if (isLoading) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.06f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = accentColor
            val spinnerPadding = size * 0.05f
            rectF.set(
                cx - size / 2f + spinnerPadding,
                cy - size / 2f + spinnerPadding,
                cx + size / 2f - spinnerPadding,
                cy + size / 2f - spinnerPadding
            )
            canvas.drawArc(rectF, rotationAngle, 90f, false, paint)
        } else if (!isRecording) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.04f
            paint.color = colorWithRawAlpha(accentColor, 208)
            val borderPadding = paint.strokeWidth / 2f
            canvas.drawCircle(cx, cy, size / 2f - borderPadding, paint)
        }

        if (pressProgress > 0f && !isRecording && !isLoading) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.08f
            paint.color = Color.argb((90 * pressProgress).toInt(), 255, 255, 255)
            canvas.drawCircle(cx, cy, size * (0.35f + 0.12f * pressProgress), paint)
        }

        if (isRecording) {
            paint.style = Paint.Style.FILL
            paint.color = contrastIconColor()
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER

            val text = formatElapsed(elapsedSeconds)
            paint.textSize = if (text.length > 3) size * 0.21f else size * 0.28f

            val fontMetrics = paint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(text, cx, textY, paint)
        } else {
            paint.color = contrastIconColor()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.046f
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND

            when (style) {
                OverlayAppearanceStore.STYLE_MIC -> drawMicIcon(canvas, cx, cy, size)
                OverlayAppearanceStore.STYLE_DOT -> drawDotIcon(canvas, cx, cy, size)
                OverlayAppearanceStore.STYLE_CUSTOM_IMAGE -> {
                    val customImageFile = OverlayAppearanceStore.customImageFile(context)
                    if (!customImageFile.exists()) {
                        drawGalleryIcon(canvas, cx, cy, size)
                    }
                }
                else -> drawWaveIcon(canvas, cx, cy, size)
            }
        }
    }

    fun applyAppearance(appearance: OverlayAppearance) {
        accentColor = appearance.accentColor
        opacityPercent = appearance.opacityPercent
        style = appearance.style
        invalidateSelf()
    }

    private fun drawWaveIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.055f
        paint.strokeCap = Paint.Cap.ROUND
        
        val gap = size * 0.10f
        val h1 = size * 0.16f
        val h2 = size * 0.32f
        val h3 = size * 0.44f

        canvas.drawLine(cx - gap * 2f, cy - h1 / 2f, cx - gap * 2f, cy + h1 / 2f, paint)
        canvas.drawLine(cx - gap, cy - h2 / 2f, cx - gap, cy + h2 / 2f, paint)
        canvas.drawLine(cx, cy - h3 / 2f, cx, cy + h3 / 2f, paint)
        canvas.drawLine(cx + gap, cy - h2 / 2f, cx + gap, cy + h2 / 2f, paint)
        canvas.drawLine(cx + gap * 2f, cy - h1 / 2f, cx + gap * 2f, cy + h1 / 2f, paint)
    }

    private fun drawMicIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val scale = (size * 0.60f) / 24f
        fun mx(x: Float) = cx + (x - 12f) * scale
        fun my(y: Float) = cy + (y - 12f) * scale

        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        // Capsule Path (Stroke)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.046f
        val path1 = android.graphics.Path().apply {
            moveTo(mx(14.75f), my(7.33303f))
            lineTo(mx(14.75f), my(11.222f))
            cubicTo(mx(14.7728f), my(12.4877f), mx(13.7657f), my(13.5325f), mx(12.5f), my(13.556f))
            cubicTo(mx(11.2343f), my(13.5325f), mx(10.2271f), my(12.4877f), mx(10.25f), my(11.222f))
            lineTo(mx(10.25f), my(7.33303f))
            cubicTo(mx(10.2277f), my(6.06772f), mx(11.2347f), my(5.02357f), mx(12.5f), my(5.00003f))
            cubicTo(mx(13.7653f), my(5.02357f), mx(14.7723f), my(6.06772f), mx(14.75f), my(7.33303f))
            close()
        }
        canvas.drawPath(path1, paint)

        // Cradle & Stand Path (Fill)
        paint.style = Paint.Style.FILL
        val path2 = android.graphics.Path().apply {
            moveTo(mx(7.53767f), my(15.0346f))
            cubicTo(mx(10.4524f), my(17.3164f), mx(14.5476f), my(17.3164f), mx(17.4623f), my(15.0346f))
            lineTo(mx(16.5377f), my(13.8534f))
            cubicTo(mx(14.1661f), my(15.7101f), mx(10.8339f), my(15.7101f), mx(8.46233f), my(13.8534f))
            lineTo(mx(7.53767f), my(15.0346f))
            close()

            moveTo(mx(11.75f), my(16f))
            lineTo(mx(13.25f), my(16f))
            lineTo(mx(13.25f), my(19f))
            lineTo(mx(11.75f), my(19f))
            close()
        }
        canvas.drawPath(path2, paint)
    }

    private fun drawDotIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, size * 0.13f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.05f
        canvas.drawCircle(cx, cy, size * 0.30f, paint)
    }

    private fun drawGalleryIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val scale = (size * 0.60f) / 24f
        fun mx(x: Float) = cx + (x - 12f) * scale
        fun my(y: Float) = cy + (y - 12f) * scale

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.046f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val path = android.graphics.Path().apply {
            moveTo(mx(14.2639f), my(15.9375f))
            lineTo(mx(12.5958f), my(14.2834f))
            cubicTo(mx(11.7909f), my(13.4851f), mx(11.3884f), my(13.086f), mx(10.9266f), my(12.9401f))
            cubicTo(mx(10.5204f), my(12.8118f), mx(10.0838f), my(12.8165f), mx(9.68048f), my(12.9536f))
            cubicTo(mx(9.22188f), my(13.1095f), mx(8.82814f), my(13.5172f), mx(8.04068f), my(14.3326f))
            lineTo(mx(4.04409f), my(18.2801f))

            moveTo(mx(14.2639f), my(15.9375f))
            lineTo(mx(14.6053f), my(15.599f))
            cubicTo(mx(15.4112f), my(14.7998f), mx(15.8141f), my(14.4002f), mx(16.2765f), my(14.2543f))
            cubicTo(mx(16.6831f), my(14.126f), mx(17.12f), my(14.1311f), mx(17.5236f), my(14.2687f))
            cubicTo(mx(17.9824f), my(14.4251f), mx(18.3761f), my(14.8339f), mx(19.1634f), my(15.6514f))
            lineTo(mx(20.0f), my(16.4934f))

            moveTo(mx(14.2639f), my(15.9375f))
            lineTo(mx(18.275f), my(19.9565f))

            moveTo(mx(18.275f), my(19.9565f))
            cubicTo(mx(17.9176f), my(20f), mx(17.4543f), my(20f), mx(16.8f), my(20f))
            lineTo(mx(7.2f), my(20f))
            cubicTo(mx(6.07989f), my(20f), mx(5.51984f), my(20f), mx(5.09202f), my(19.782f))
            cubicTo(mx(4.71569f), my(19.5903f), mx(4.40973f), my(19.2843f), mx(4.21799f), my(18.908f))
            cubicTo(mx(4.12796f), my(18.7313f), mx(4.07512f), my(18.5321f), mx(4.04409f), my(18.2801f))

            moveTo(mx(18.275f), my(19.9565f))
            cubicTo(mx(18.5293f), my(19.9256f), mx(18.7301f), my(19.8727f), mx(18.908f), my(19.782f))
            cubicTo(mx(19.2843f), my(19.5903f), mx(19.5903f), my(19.2843f), mx(19.782f), my(18.908f))
            cubicTo(mx(20f), my(18.4802f), mx(20f), my(17.9201f), mx(20f), my(16.8f))
            lineTo(mx(20f), my(16.4934f))

            moveTo(mx(4.04409f), my(18.2801f))
            cubicTo(mx(4f), my(17.9221f), mx(4f), my(17.4575f), mx(4f), my(16.8f))
            lineTo(mx(4f), my(7.2f))
            cubicTo(mx(4f), my(6.0799f), mx(4f), my(5.51984f), mx(4.21799f), my(5.09202f))
            cubicTo(mx(4.40973f), my(4.71569f), mx(4.71569f), my(4.40973f), mx(5.09202f), my(4.21799f))
            cubicTo(mx(5.51984f), my(4f), mx(6.07989f), my(4f), mx(7.2f), my(4f))
            lineTo(mx(16.8f), my(4f))
            cubicTo(mx(17.9201f), my(4f), mx(18.4802f), my(4f), mx(18.908f), my(4.21799f))
            cubicTo(mx(19.2843f), my(4.40973f), mx(19.5903f), my(4.71569f), mx(19.782f), my(5.09202f))
            cubicTo(mx(20f), my(5.51984f), mx(20f), my(6.0799f), mx(20f), my(7.2f))
            lineTo(mx(20f), my(16.4934f))

            moveTo(mx(17f), my(8.99989f))
            cubicTo(mx(17f), my(10.1045f), mx(16.1046f), my(10.9999f), mx(15f), my(10.9999f))
            cubicTo(mx(13.8954f), my(10.9999f), mx(13f), my(10.1045f), mx(13f), my(8.99989f))
            cubicTo(mx(13f), my(7.89532f), mx(13.8954f), my(6.99989f), mx(15f), my(6.99989f))
            cubicTo(mx(16.1046f), my(6.99989f), mx(17f), my(7.89532f), mx(17f), my(8.99989f))
        }
        canvas.drawPath(path, paint)
    }

    private fun colorWithRawAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun contrastIconColor(): Int {
        return if (accentColor == Color.WHITE || accentColor == Color.parseColor("#F4F4F5")) {
            Color.parseColor("#111827")
        } else {
            Color.WHITE
        }
    }

    private fun formatElapsed(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
