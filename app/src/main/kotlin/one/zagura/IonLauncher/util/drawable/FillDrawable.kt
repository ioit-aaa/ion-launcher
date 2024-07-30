package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.graphics.alpha

class FillDrawable(color: Int) : Drawable() {

    var color: Int = color
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(color)
    }

    override fun getOpacity() = if (color.alpha == 0xff) PixelFormat.OPAQUE else PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        color = color and 0xffffff or (alpha shl 24)
        invalidateSelf()
    }
    override fun getAlpha() = color.alpha
    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getMinimumWidth() = 0
    override fun getMinimumHeight() = 0
}