package com.lagradost.cloudstream3.ui.home

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class HomeScrollTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // Keep hero motion on compositor-friendly properties. The previous
        // implementation changed padding for every scroll frame, forcing
        // repeated layout work and preventing high-refresh panels from making
        // the interaction feel as direct as the rest of Vanta.
        val distance = abs(position).coerceIn(0f, 1f)
        val scale = 1f - 0.025f * distance

        page.translationX = -position * page.width * 0.10f
        page.scaleX = scale
        page.scaleY = scale
        page.alpha = 1f - 0.18f * distance
    }
}
