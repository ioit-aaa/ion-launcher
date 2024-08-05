package one.zagura.IonLauncher.util.drawable

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.toXfermode

/** Only meant to be used inside [ClippedDrawable] */
class IconGlossDrawable(val drawable: Drawable) : Drawable() {

    @SuppressLint("CanvasSize")
    override fun draw(canvas: Canvas) {
        drawable.bounds = bounds
        drawable.draw(canvas)
        canvas.drawPaint(Paint().apply {
            xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
            shader = LinearGradient(0f, canvas.height * 0.2f, 0f, canvas.height.toFloat(), 0x44ffffff, 0x22000000, Shader.TileMode.CLAMP)
        })
        canvas.drawOval(
            -canvas.width * 0.6f,
            -canvas.height.toFloat(),
            canvas.width * 1.6f,
            canvas.height * 0.5f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, canvas.height * 0.5f, 0x22ffffff, 0x11ffffff, Shader.TileMode.CLAMP)
            },
        )
        canvas.drawOval(
            -canvas.width * 0.6f,
            -canvas.height.toFloat(),
            canvas.width * 1.6f,
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