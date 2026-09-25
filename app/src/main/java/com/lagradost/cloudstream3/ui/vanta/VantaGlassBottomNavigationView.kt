package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Phone navigation that paints the shared Vanta backdrop before its icons. */
class VantaGlassBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle,
) : BottomNavigationView(context, attrs, defStyleAttr) {
    private val glass = VantaGlassPainter(this)

    init {
        setWillNotDraw(false)
        background = null
        clipToOutline = true
        outlineProvider = glass.outlineProvider
    }

    override fun onDraw(canvas: Canvas) {
        glass.draw(canvas, useBackdrop = true)
        super.onDraw(canvas)
    }
}
