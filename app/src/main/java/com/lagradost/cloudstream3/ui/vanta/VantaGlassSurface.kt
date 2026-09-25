package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.lagradost.cloudstream3.R

/**
 * Reusable Vanta control surface.
 *
 * API 31+ uses hardware RenderEffect for the surface content and keeps the
 * shell translucent so sampled artwork can visually influence the control.
 * Older Android versions receive the lightweight Vanta fallback drawable.
 *
 * This class deliberately avoids software bitmap blur in the draw loop.
 */
class VantaGlassSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
        background = context.getDrawable(R.drawable.vanta_glass_control)
        clipToOutline = true
        elevation = 10f * resources.displayMetrics.density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    /**
     * Applies a GPU blur to content hosted by this surface when desired.
     * True backdrop sampling is handled by the parent glass host rather than
     * repeatedly copying the window into bitmaps.
     */
    fun setGlassBlurEnabled(enabled: Boolean, radiusDp: Float = 18f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = radiusDp * resources.displayMetrics.density
            setRenderEffect(
                if (enabled) RenderEffect.createBlurEffect(
                    radius,
                    radius,
                    Shader.TileMode.CLAMP
                ) else null
            )
        }
    }

    fun setPressedVisual(pressed: Boolean) {
        alpha = if (pressed) 0.92f else 1f
        scaleX = if (pressed) 0.975f else 1f
        scaleY = if (pressed) 0.975f else 1f
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        setPressedVisual(isPressed)
    }
}
