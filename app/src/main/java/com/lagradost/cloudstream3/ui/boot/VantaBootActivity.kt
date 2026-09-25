package com.lagradost.cloudstream3.ui.boot

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity

/**
 * Vanta's dedicated cold-start presentation layer.
 *
 * The boot choreography is intentionally authored on a 60 fps timeline.
 * Normal application motion remains display-refresh-native.
 *
 * Branding artwork is kept in a replaceable drawable slot so the locked
 * VANTA Speed master can be dropped in without changing launch routing.
 */
class VantaBootActivity : Activity() {
    companion object {
        private const val BOOT_DURATION_MS = 900L
        private const val WORDMARK_REVEAL_MS = 420L
        private const val HOLD_MS = 180L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val streaks = ImageView(this).apply {
            setImageResource(R.drawable.vanta_boot_streaks)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
            translationX = -96f
        }

        val wordmark = ImageView(this).apply {
            setImageResource(R.drawable.vanta_wordmark_speed)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
            scaleX = 0.985f
            scaleY = 0.985f
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.vanta_boot_art_height)
        ).apply {
            gravity = Gravity.CENTER
            marginStart = resources.getDimensionPixelSize(R.dimen.vanta_space_6)
            marginEnd = resources.getDimensionPixelSize(R.dimen.vanta_space_6)
        }

        root.addView(streaks, lp)
        root.addView(wordmark, lp)
        setContentView(root)

        val streakIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(streaks, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(streaks, View.TRANSLATION_X, -96f, 0f)
            )
            duration = 260L
            interpolator = DecelerateInterpolator(1.8f)
        }

        val resolve = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(wordmark, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(wordmark, View.SCALE_X, 0.985f, 1f),
                ObjectAnimator.ofFloat(wordmark, View.SCALE_Y, 0.985f, 1f),
                // Excess streaks continue beneath the wordmark and vanish.
                ObjectAnimator.ofFloat(streaks, View.TRANSLATION_X, 0f, 54f),
                ObjectAnimator.ofFloat(streaks, View.ALPHA, 1f, 0f)
            )
            duration = WORDMARK_REVEAL_MS
            interpolator = DecelerateInterpolator(2.2f)
        }

        AnimatorSet().apply {
            playSequentially(streakIn, resolve)
            startDelay = 40L
            doOnEnd {
                root.postDelayed({ forwardToApp() }, HOLD_MS)
            }
            start()
        }

        // Safety valve. Boot presentation must never trap the user.
        root.postDelayed({
            if (!isFinishing) forwardToApp()
        }, BOOT_DURATION_MS + 250L)
    }

    private fun forwardToApp() {
        if (isFinishing) return

        val next = Intent(this, AccountSelectActivity::class.java).apply {
            action = intent.action
            data = intent.data
            type = intent.type
            intent.extras?.let(::putExtras)
            intent.clipData?.let { clipData = it }
            flags = intent.flags
        }
        startActivity(next)
        finish()
        overridePendingTransition(0, 0)
    }
}
