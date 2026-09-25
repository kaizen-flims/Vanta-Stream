package com.lagradost.cloudstream3.ui.vanta

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.lagradost.cloudstream3.R

/**
 * Records the content layer once per rendered frame so Vanta glass controls can
 * sample the actual scene behind them without allocating software bitmaps.
 *
 * Android 12+ keeps the complete path on the GPU through RenderNode and
 * RenderEffect. Earlier versions simply expose no backdrop and consumers draw
 * the opaque Vanta fallback surface.
 */
class VantaBackdropLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), VantaBackdropProvider {

    override var isRecordingVantaBackdrop: Boolean = false
        private set

    private val renderer: BackdropRenderer? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Api31BackdropRenderer(
            blurRadiusPx = resources.getDimension(R.dimen.vanta_glass_blur_radius)
        )
    } else {
        null
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (canvas.isHardwareAccelerated && width > 0 && height > 0) {
            findViewById<View>(R.id.nav_host_fragment)?.let { source ->
                isRecordingVantaBackdrop = true
                try {
                    renderer?.record(this, source)
                } finally {
                    isRecordingVantaBackdrop = false
                }
            }
        }
        super.dispatchDraw(canvas)
    }

    override fun drawVantaBackdrop(canvas: Canvas, consumer: View): Boolean {
        if (!canvas.isHardwareAccelerated || !consumer.isShown) return false
        return renderer?.drawInto(this, canvas, consumer) == true
    }

    override fun onDetachedFromWindow() {
        renderer?.release()
        super.onDetachedFromWindow()
    }
}

internal interface VantaBackdropProvider {
    val isRecordingVantaBackdrop: Boolean
    fun drawVantaBackdrop(canvas: Canvas, consumer: View): Boolean
}

private interface BackdropRenderer {
    fun record(host: View, source: View)
    fun drawInto(host: View, canvas: Canvas, consumer: View): Boolean
    fun release()
}

@RequiresApi(Build.VERSION_CODES.S)
private class Api31BackdropRenderer(
    blurRadiusPx: Float,
) : BackdropRenderer {
    private val renderNode = RenderNode("VantaBackdrop")
    private val hostLocation = IntArray(2)
    private val sourceLocation = IntArray(2)
    private val consumerLocation = IntArray(2)
    private val sourceBounds = Rect()

    init {
        val blur = RenderEffect.createBlurEffect(
            blurRadiusPx,
            blurRadiusPx,
            Shader.TileMode.CLAMP,
        )
        renderNode.setRenderEffect(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33GlassEffect.chainWith(blur)
            } else {
                blur
            }
        )
    }

    override fun record(host: View, source: View) {
        if (!source.isShown || host.width <= 0 || host.height <= 0) return

        host.getLocationInWindow(hostLocation)
        source.getLocationInWindow(sourceLocation)
        val sourceLeft = sourceLocation[0] - hostLocation[0]
        val sourceTop = sourceLocation[1] - hostLocation[1]
        sourceBounds.set(
            sourceLeft,
            sourceTop,
            sourceLeft + source.width,
            sourceTop + source.height,
        )

        renderNode.setPosition(0, 0, host.width, host.height)
        val recordingCanvas = renderNode.beginRecording(host.width, host.height)
        val save = recordingCanvas.save()
        recordingCanvas.translate(sourceLeft.toFloat(), sourceTop.toFloat())
        source.draw(recordingCanvas)
        recordingCanvas.restoreToCount(save)
        renderNode.endRecording()
    }

    override fun drawInto(host: View, canvas: Canvas, consumer: View): Boolean {
        if (!renderNode.hasDisplayList() || sourceBounds.isEmpty) return false

        host.getLocationInWindow(hostLocation)
        consumer.getLocationInWindow(consumerLocation)
        val consumerLeft = consumerLocation[0] - hostLocation[0]
        val consumerTop = consumerLocation[1] - hostLocation[1]

        // Floating navigation sits just outside the content container today.
        // Clamp its sampling window to the nearest real content pixels so its
        // tint still reacts to the currently displayed poster/scene.
        val maxSampleLeft = (sourceBounds.right - consumer.width).coerceAtLeast(sourceBounds.left)
        val maxSampleTop = (sourceBounds.bottom - consumer.height).coerceAtLeast(sourceBounds.top)
        val sampleLeft = consumerLeft.coerceIn(sourceBounds.left, maxSampleLeft)
        val sampleTop = consumerTop.coerceIn(sourceBounds.top, maxSampleTop)

        val save = canvas.save()
        canvas.translate(-sampleLeft.toFloat(), -sampleTop.toFloat())
        canvas.drawRenderNode(renderNode)
        canvas.restoreToCount(save)
        return true
    }

    override fun release() {
        renderNode.discardDisplayList()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33GlassEffect {
    // A sub-pixel optical displacement keeps the surface alive without making
    // text or poster art visibly swim. The backdrop colour itself drives the
    // resulting tint, so red and blue scenes remain perceptibly different.
    private const val GLASS_SHADER = """
        uniform shader content;

        half4 main(float2 position) {
            float horizontal = sin(position.y * 0.031) * 0.72;
            float vertical = sin(position.x * 0.019) * 0.28;
            half4 sampled = content.eval(position + float2(horizontal, vertical));
            half luminance = dot(sampled.rgb, half3(0.2126, 0.7152, 0.0722));
            half veil = mix(0.105, 0.045, smoothstep(0.0, 1.0, luminance));
            half3 adaptive = mix(sampled.rgb, half3(0.98, 0.98, 1.0), veil);
            return half4(adaptive, sampled.a);
        }
    """

    fun chainWith(blur: RenderEffect): RenderEffect = try {
        val shader = RuntimeShader(GLASS_SHADER)
        val refraction = RenderEffect.createRuntimeShaderEffect(shader, "content")
        RenderEffect.createChainEffect(refraction, blur)
    } catch (_: RuntimeException) {
        blur
    }
}
