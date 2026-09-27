package com.lagradost.cloudstream3.ui.vanta

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lagradost.cloudstream3.R

fun Dialog.applyVantaWindowGlass() {
    val dialogWindow = window ?: return
    val density = context.resources.displayMetrics.density
    dialogWindow.decorView.post {
        val accessibility = ContextCompat.getSystemService(context, AccessibilityManager::class.java)
        val highContrast = if (Build.VERSION.SDK_INT >= 36) accessibility?.isHighContrastTextEnabled == true
        else Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !highContrast && windowManager?.isCrossWindowBlurEnabled == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (canBlur) {
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    blurBehindRadius = context.resources.getDimensionPixelSize(R.dimen.vanta_glass_blur_radius)
                    dimAmount = 0.18f
                }
            } else dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
        val fill = ContextCompat.getColor(context, if (canBlur) R.color.vanta_glass_window_fill else R.color.vanta_surface_elevated)
        val edge = ContextCompat.getColor(context, R.color.vanta_glass_border)
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE; cornerRadius = 24f * density
            setColor(fill); setStroke(density.toInt().coerceAtLeast(1), edge)
        }
        if (this is BottomSheetDialog) {
            dialogWindow.setBackgroundDrawableResource(android.R.color.transparent)
            findViewById<android.view.View>(MaterialR.id.design_bottom_sheet)?.background = background
        } else dialogWindow.setBackgroundDrawable(background)
    }
}
