package com.lagradost.cloudstream3.ui.boot

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import kotlin.math.min

/**
 * Vanta cold-start presentation.
 *
 * This screen intentionally updates visual state on a 60 fps cadence
 * (16.67 ms). All normal Vanta UI motion remains display-refresh-native.
 */
class VantaBootActivity : Activity(), Choreographer.FrameCallback {
    companion object {
        private const val FRAME_60_NS = 16_666_667L
        private const val STREAK_END_MS = 260f
        private const val RESOLVE_END_MS = 680f
        private const val FINISH_MS = 860f
    }

    private lateinit var streaks: ImageView
    private lateinit var wordmark: ImageView
    private var startNs = 0L
    private var lastRenderedNs = Long.MIN_VALUE
    private var forwarded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        streaks = ImageView(this).apply {
            setImageResource(R.drawable.vanta_boot_streaks)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
        }
        wordmark = ImageView(this).apply {
            setImageResource(R.drawable.vanta_wordmark_speed)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
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

        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (startNs == 0L) startNs = frameTimeNanos

        // Choreographer may callback at 90/120/144 Hz. Only mutate boot visuals
        // when the 60 Hz frame interval has elapsed.
        if (lastRenderedNs == Long.MIN_VALUE || frameTimeNanos - lastRenderedNs >= FRAME_60_NS) {
            lastRenderedNs = frameTimeNanos
            renderFrame((frameTimeNanos - startNs) / 1_000_000f)
        }

        if (!forwarded) Choreographer.getInstance().postFrameCallback(this)
    }

    private fun renderFrame(ms: Float) {
        when {
            ms <= STREAK_END_MS -> {
                val t = easeOut(ms / STREAK_END_MS)
                streaks.alpha = t
                streaks.translationX = lerp(-96f, 0f, t)
                wordmark.alpha = 0f
            }
            ms <= RESOLVE_END_MS -> {
                val t = easeOut((ms - STREAK_END_MS) / (RESOLVE_END_MS - STREAK_END_MS))
                wordmark.alpha = t
                val scale = lerp(0.985f, 1f, t)
                wordmark.scaleX = scale
                wordmark.scaleY = scale

                // Streaks continue underneath the resolving artwork, then disappear.
                streaks.translationX = lerp(0f, 54f, t)
                streaks.alpha = 1f - t
            }
            ms >= FINISH_MS -> forwardToApp()
        }
    }

    private fun easeOut(value: Float): Float {
        val t = min(1f, value.coerceAtLeast(0f))
        val inv = 1f - t
        return 1f - inv * inv * inv
    }

    private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t

    private fun forwardToApp() {
        if (forwarded || isFinishing) return
        forwarded = true

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

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDestroy()
    }
}
