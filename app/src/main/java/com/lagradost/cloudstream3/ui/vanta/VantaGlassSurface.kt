package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewParent
import android.view.accessibility.AccessibilityManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.lagradost.cloudstream3.R

/** Hardware-backed, backdrop-reactive glass intended only for controls and navigation. */
class VantaGlassSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val glass: VantaGlassPainter
    private var blurEnabled: Boolean
    private var lastPressedVisual = false

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.VantaGlassSurface, defStyleAttr, 0)
        val radius = attributes.getDimension(
            R.styleable.VantaGlassSurface_vantaGlassCornerRadius,
            resources.getDimension(R.dimen.vanta_radius_pill),
        )
        blurEnabled = attributes.getBoolean(R.styleable.VantaGlassSurface_vantaGlassBackdropEnabled, true)
        val surfaceElevation = attributes.getDimension(
            R.styleable.VantaGlassSurface_vantaGlassElevation,
            10f * resources.displayMetrics.density,
        )
        attributes.recycle()
        glass = VantaGlassPainter(this, radius)
        setWillNotDraw(false)
        background = null
        clipToOutline = true
        outlineProvider = glass.outlineProvider
        elevation = surfaceElevation
    }

    @Suppress("UNUSED_PARAMETER")
    fun setGlassBlurEnabled(enabled: Boolean, radiusDp: Float = 18f) {
        blurEnabled = enabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!glass.isBackdropCaptureInProgress()) glass.draw(canvas, blurEnabled)
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (glass.isBackdropCaptureInProgress()) return
        super.dispatchDraw(canvas)
    }

    private fun setPressedVisual(pressed: Boolean) {
        if (lastPressedVisual == pressed) return
        lastPressedVisual = pressed
        animate().cancel()
        if (!VantaMotion.animationsEnabled(context)) {
            alpha = 1f; scaleX = 1f; scaleY = 1f
            return
        }
        animate().alpha(if (pressed) 0.92f else 1f)
            .scaleX(if (pressed) 0.975f else 1f).scaleY(if (pressed) 0.975f else 1f)
            .setDuration(resources.getInteger(R.integer.vanta_motion_fast).toLong())
            .setInterpolator(AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in))
            .start()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        setPressedVisual(isPressed)
    }
}

internal class VantaGlassPainter(private val view: View, private val radius: Float) {
    private val stroke = view.resources.getDimension(R.dimen.vanta_glass_stroke)
    private val bounds = RectF()
    private val clipPath = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke }
    private var geometryWidth = -1
    private var geometryHeight = -1
    private var edgeShader: Shader? = null
    private val sampledFill = ContextCompat.getColor(view.context, R.color.vanta_glass_fill)
    private val sampledFillPressed = ContextCompat.getColor(view.context, R.color.vanta_glass_fill_pressed)
    private val fallbackFill = ContextCompat.getColor(view.context, R.color.vanta_surface_elevated)
    private val accessibilityManager = ContextCompat.getSystemService(view.context, AccessibilityManager::class.java)

    val outlineProvider: ViewOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(target: View, outline: android.graphics.Outline) =
            outline.setRoundRect(0, 0, target.width, target.height, radius)
    }

    private fun ensureGeometry() {
        if (geometryWidth == view.width && geometryHeight == view.height) return
        geometryWidth = view.width; geometryHeight = view.height
        bounds.set(0f, 0f, view.width.toFloat(), view.height.toFloat())
        clipPath.rewind(); clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW)
        edgeShader = LinearGradient(
            0f, 0f, view.width.toFloat(), view.height.toFloat(),
            intArrayOf(Color.argb(150,255,255,255), Color.argb(28,255,255,255), Color.argb(72,255,255,255)),
            floatArrayOf(0f, 0.54f, 1f), Shader.TileMode.CLAMP,
        )
    }

    fun draw(canvas: Canvas, useBackdrop: Boolean) {
        if (view.width <= 0 || view.height <= 0) return
        ensureGeometry()
        val highContrast = if (Build.VERSION.SDK_INT >= 36) {
            accessibilityManager?.isHighContrastTextEnabled == true
        } else Settings.Secure.getInt(view.context.contentResolver, "high_text_contrast_enabled", 0) == 1
        val save = canvas.save(); canvas.clipPath(clipPath)
        val sampled = !highContrast && useBackdrop && findBackdropProvider(view.parent)?.drawVantaBackdrop(canvas, view) == true
        fillPaint.color = when {
            sampled && view.isPressed -> sampledFillPressed
            sampled -> sampledFill
            else -> ColorUtils.setAlphaComponent(fallbackFill, if (highContrast) 255 else 242)
        }
        canvas.drawRoundRect(bounds, radius, radius, fillPaint); canvas.restoreToCount(save)
        edgePaint.shader = edgeShader
        val inset = stroke / 2f; bounds.inset(inset, inset)
        canvas.drawRoundRect(bounds, radius - inset, radius - inset, edgePaint)
        bounds.inset(-inset, -inset); edgePaint.shader = null
    }

    fun isBackdropCaptureInProgress() = findBackdropProvider(view.parent)?.isRecordingVantaBackdrop == true
    private fun findBackdropProvider(parent: ViewParent?): VantaBackdropProvider? {
        var current = parent
        while (current != null) {
            if (current is VantaBackdropProvider) return current
            current = current.parent
        }
        return null
    }
}
