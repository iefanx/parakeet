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
        val logoSize = size * 0.70f
        val scale = logoSize / 24f
        fun mapX(x: Float): Float = cx + (x - 12f) * scale
        fun mapY(y: Float): Float = cy + (y - 12f) * scale

        canvas.drawLine(mapX(6f), mapY(11f), mapX(6f), mapY(13f), paint)
        canvas.drawLine(mapX(9f), mapY(9f), mapX(9f), mapY(15f), paint)
        canvas.drawLine(mapX(12f), mapY(6f), mapX(12f), mapY(18f), paint)
        canvas.drawLine(mapX(15f), mapY(9f), mapX(15f), mapY(15f), paint)
        canvas.drawLine(mapX(18f), mapY(11f), mapX(18f), mapY(13f), paint)
        canvas.drawCircle(cx, cy, 9f * scale, paint)
    }

    private fun drawMicIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.055f
        val micBody = RectF(cx - size * 0.13f, cy - size * 0.24f, cx + size * 0.13f, cy + size * 0.13f)
        canvas.drawRoundRect(micBody, size * 0.12f, size * 0.12f, paint)
        canvas.drawArc(
            RectF(cx - size * 0.26f, cy - size * 0.04f, cx + size * 0.26f, cy + size * 0.34f),
            0f,
            180f,
            false,
            paint
        )
        canvas.drawLine(cx, cy + size * 0.32f, cx, cy + size * 0.43f, paint)
        canvas.drawLine(cx - size * 0.14f, cy + size * 0.43f, cx + size * 0.14f, cy + size * 0.43f, paint)
    }

    private fun drawDotIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, size * 0.13f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.05f
        canvas.drawCircle(cx, cy, size * 0.30f, paint)
    }

    private fun drawGalleryIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.046f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val frame = RectF(cx - size * 0.18f, cy - size * 0.14f, cx + size * 0.18f, cy + size * 0.14f)
        canvas.drawRoundRect(frame, size * 0.04f, size * 0.04f, paint)

        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - size * 0.07f, cy - size * 0.04f, size * 0.035f, paint)

        paint.style = Paint.Style.STROKE
        val path = android.graphics.Path().apply {
            moveTo(cx - size * 0.13f, cy + size * 0.12f)
            lineTo(cx - size * 0.03f, cy - size * 0.02f)
            lineTo(cx + size * 0.05f, cy + size * 0.06f)
            lineTo(cx + size * 0.09f, cy + size * 0.01f)
            lineTo(cx + size * 0.14f, cy + size * 0.12f)
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
