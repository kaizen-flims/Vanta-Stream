package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewParent
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.lagradost.cloudstream3.R

/**
 * Reusable Vanta control surface.
 *
 * Backdrop rendering is supplied by [VantaBackdropLayout]. This surface never
 * blurs its own children and never copies the window into software bitmaps.
 */
class VantaGlassSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val glass = VantaGlassPainter(this)
    private var blurEnabled = true

    init {
        setWillNotDraw(false)
        background = null
        clipToOutline = true
        outlineProvider = glass.outlineProvider
        elevation = 10f * resources.displayMetrics.density
    }

    /**
     * Controls whether this surface samples the shared hardware backdrop.
     * [radiusDp] remains source-compatible while blur radius is owned by the
     * host so every glass control shares one GPU render pass.
     */
    @Suppress("UNUSED_PARAMETER")
    fun setGlassBlurEnabled(enabled: Boolean, radiusDp: Float = 18f) {
        blurEnabled = enabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!glass.isBackdropCaptureInProgress()) {
            glass.draw(canvas, useBackdrop = blurEnabled)
        }
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (glass.isBackdropCaptureInProgress()) return
        super.dispatchDraw(canvas)
    }

    fun setPressedVisual(pressed: Boolean) {
        animate()
            .alpha(if (pressed) 0.92f else 1f)
            .scaleX(if (pressed) 0.975f else 1f)
            .scaleY(if (pressed) 0.975f else 1f)
            .setDuration(resources.getInteger(R.integer.vanta_motion_fast).toLong())
            .setInterpolator(
                AnimationUtils.loadInterpolator(
                    context,
                    android.R.interpolator.fast_out_slow_in,
                )
            )
            .start()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        setPressedVisual(isPressed)
    }
}

internal class VantaGlassPainter(private val view: View) {
    private val radius = view.resources.getDimension(R.dimen.vanta_radius_pill)
    private val stroke = view.resources.getDimension(R.dimen.vanta_glass_stroke)
    private val bounds = RectF()
    private val clipPath = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }

    val outlineProvider: ViewOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(target: View, outline: android.graphics.Outline) {
            outline.setRoundRect(0, 0, target.width, target.height, radius)
        }
    }

    fun draw(canvas: Canvas, useBackdrop: Boolean) {
        if (view.width <= 0 || view.height <= 0) return

        bounds.set(0f, 0f, view.width.toFloat(), view.height.toFloat())
        clipPath.rewind()
        clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW)

        val save = canvas.save()
        canvas.clipPath(clipPath)
        val sampled = useBackdrop && findBackdropProvider(view.parent)
            ?.drawVantaBackdrop(canvas, view) == true

        fillPaint.color = if (sampled) {
            Color.argb(36, 255, 255, 255)
        } else {
            Color.argb(238, 18, 18, 20)
        }
        canvas.drawRoundRect(bounds, radius, radius, fillPaint)
        canvas.restoreToCount(save)

        edgePaint.shader = LinearGradient(
            0f,
            0f,
            view.width.toFloat(),
            view.height.toFloat(),
            intArrayOf(
                Color.argb(150, 255, 255, 255),
                Color.argb(28, 255, 255, 255),
                Color.argb(72, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.54f, 1f),
            Shader.TileMode.CLAMP,
        )
        val inset = stroke / 2f
        bounds.inset(inset, inset)
        canvas.drawRoundRect(bounds, radius - inset, radius - inset, edgePaint)
        edgePaint.shader = null
    }

    fun isBackdropCaptureInProgress(): Boolean =
        findBackdropProvider(view.parent)?.isRecordingVantaBackdrop == true

    private fun findBackdropProvider(parent: ViewParent?): VantaBackdropProvider? {
        var current = parent
        while (current != null) {
            if (current is VantaBackdropProvider) return current
            current = current.parent
        }
        return null
    }
}
