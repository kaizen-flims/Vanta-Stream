package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.SearchView

/** Search control with a content-reactive Vanta glass background. */
class VantaGlassSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle,
) : SearchView(context, attrs, defStyleAttr) {
    private val glass = VantaGlassPainter(this)

    init {
        setWillNotDraw(false)
        background = null
        clipToOutline = true
        outlineProvider = glass.outlineProvider
    }

    override fun onDraw(canvas: Canvas) {
        if (!glass.isBackdropCaptureInProgress()) {
            glass.draw(canvas, useBackdrop = true)
        }
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (glass.isBackdropCaptureInProgress()) return
        super.dispatchDraw(canvas)
    }
}
