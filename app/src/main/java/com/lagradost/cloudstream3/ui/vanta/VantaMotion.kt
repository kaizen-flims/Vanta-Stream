package com.lagradost.cloudstream3.ui.vanta

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings

object VantaMotion {
    fun animationsEnabled(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 26) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }
    } catch (_: RuntimeException) {
        true
    }
}
