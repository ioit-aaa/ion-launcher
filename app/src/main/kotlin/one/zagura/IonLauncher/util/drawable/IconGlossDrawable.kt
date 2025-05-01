package one.zagura.IonLauncher.util.drawable

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.toXfermode

/** Only meant to be used inside [ClippedDrawable] */
class IconGlossDrawable(val drawable: Drawable, val radiusRatio: Float) : Drawable() {

    companion object {
        private val rimPaint = Paint().apply {
            xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
        }
        private val gradPaint = Paint().apply {
            xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
        }
    }

    @SuppressLint("CanvasSize")
    override fun draw(canvas: Canvas) {
        drawable.bounds = bounds
        drawable.draw(canvas)
        canvas.drawPaint(gradPaint.apply {
            shader = LinearGradient(0f, canvas.height * 0.2f, 0f, canvas.height.toFloat(), 0x44ffffff, 0x22000000, Shader.TileMode.CLAMP)
        })
        if (radiusRatio == 0.5f)
            canvas.drawPaint(rimPaint.apply {
                shader = RadialGradient(canvas.width / 2f, canvas.height / 2f, canvas.width / 2f, intArrayOf(0x00ffffff, 0x22ffffff), floatArrayOf(0.7f, 1f), Shader.TileMode.CLAMP)
            })
        canvas.drawOval(
            -canvas.width * (0.6f - radiusRatio * 1.5f),
            -canvas.height * (0.47f - radiusRatio),
            canvas.width * (1.6f - radiusRatio * 1.5f),
            canvas.height * 0.5f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, canvas.height * 0.5f, 0x2affffff, 0x0dffffff, Shader.TileMode.CLAMP)
            },
        )
        canvas.drawOval(
            -canvas.width * (0.6f - radiusRatio * 1.5f),
            -canvas.height * (0.47f - radiusRatio),
            canvas.width * (1.6f - radiusRatio * 1.5f),
            canvas.height * 0.5f,
            Paint().apply {
                this.color = 0x11ffffff
                xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
            },
        )
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth() = drawable.intrinsicWidth
    override fun getIntrinsicHeight() = drawable.intrinsicHeight
}