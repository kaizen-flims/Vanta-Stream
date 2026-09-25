package com.lagradost.cloudstream3.ui.boot

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Vanta cold-start presentation.
 *
 * This screen intentionally updates visual state on a 60 fps cadence
 * (16.67 ms). All normal Vanta UI motion remains display-refresh-native.
 */
class VantaBootActivity : Activity(), Choreographer.FrameCallback {
    companion object {
        private const val FRAME_60_NS = 16_666_667L
        private const val STREAK_END_MS = 280f
        private const val RESOLVE_END_MS = 700f
        private const val HOLD_END_MS = 850f
        private const val FINISH_MS = 980f
    }

    private lateinit var root: FrameLayout
    private lateinit var streaks: ImageView
    private lateinit var wordmark: ImageView
    private var startNs = 0L
    private var lastLogicalFrame = -1L
    private var forwarded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

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

        // The excess streak layer stays behind the locked master artwork.
        root.addView(streaks, lp)
        root.addView(wordmark, lp)
        setContentView(root)

        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (startNs == 0L) startNs = frameTimeNanos

        // Quantize the boot timeline to a true 60 fps logical clock. Comparing
        // only with the previous rendered vsync would collapse 90 Hz displays
        // to 45 fps; this frame index keeps an average 60 updates per second on
        // 60/90/120/144 Hz panels while every other Vanta screen stays native.
        val elapsedNs = (frameTimeNanos - startNs).coerceAtLeast(0L)
        val logicalFrame = elapsedNs / FRAME_60_NS
        if (logicalFrame != lastLogicalFrame) {
            lastLogicalFrame = logicalFrame
            renderFrame(logicalFrame * FRAME_60_NS / 1_000_000f)
        }

        if (!forwarded) Choreographer.getInstance().postFrameCallback(this)
    }

    private fun renderFrame(ms: Float) {
        when {
            ms <= STREAK_END_MS -> {
                val t = easeOut(ms / STREAK_END_MS)
                root.alpha = 1f
                streaks.alpha = easeOut(t)
                streaks.translationX = lerp(-180f, -8f, t)
                streaks.scaleX = lerp(1.18f, 1f, t)
                wordmark.alpha = 0f
                wordmark.clipBounds = Rect(0, 0, 0, wordmark.height)
            }
            ms <= RESOLVE_END_MS -> {
                val t = easeOut((ms - STREAK_END_MS) / (RESOLVE_END_MS - STREAK_END_MS))
                val artworkWidth = wordmark.width.coerceAtLeast(1)
                val artworkHeight = wordmark.height.coerceAtLeast(1)
                wordmark.alpha = 1f
                wordmark.clipBounds = Rect(
                    0,
                    0,
                    (artworkWidth * t).roundToInt().coerceIn(0, artworkWidth),
                    artworkHeight
                )
                wordmark.translationX = lerp(-18f, 0f, t)

                // Streaks continue underneath the resolving artwork, then disappear.
                streaks.translationX = lerp(-8f, 92f, t)
                streaks.alpha = (1f - t).coerceAtLeast(0f)
            }
            ms <= HOLD_END_MS -> {
                wordmark.clipBounds = null
                wordmark.alpha = 1f
                wordmark.translationX = 0f
                streaks.alpha = 0f
                root.alpha = 1f
            }
            ms < FINISH_MS -> {
                val t = easeOut((ms - HOLD_END_MS) / (FINISH_MS - HOLD_END_MS))
                wordmark.clipBounds = null
                streaks.alpha = 0f
                root.alpha = 1f - t
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
