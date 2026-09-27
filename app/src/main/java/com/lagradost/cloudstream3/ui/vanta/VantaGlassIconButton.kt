package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class VantaGlassIconButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {
    init {
        val minimum = (48f * resources.displayMetrics.density).toInt()
        minimumWidth = minimum; minimumHeight = minimum; background = null
    }
}
