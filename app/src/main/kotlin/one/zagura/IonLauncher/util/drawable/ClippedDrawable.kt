package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.core.graphics.alpha
import androidx.core.graphics.record
import androidx.core.graphics.toXfermode

internal class ClippedDrawable(
    private val content: Drawable,
    path: Path,
    color: Int,
    is3D: Boolean,
) : Drawable() {

    companion object {
        fun Canvas.drawRim(path: Path) {
            val rimPaint = Paint().apply {
                this.color = 0x77ffffff
                xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
            }
            val rimShadowShPaint = Paint().apply {
                this.color = 0x44ffffff
                xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
            }
            val rimBorderPaint = Paint().apply {
                this.color = 0x11eeeeee
            }

            path.fillType = Path.FillType.INVERSE_EVEN_ODD
            save()
            scale(0.95f, 0.95f, width / 2f, height / 2f)
            drawPath(path, rimBorderPaint)
            restore()

            save()
            translate(0f, height * 0.025f)
            drawPath(path, rimPaint)
            translate(0f, -height * 0.05f)
            drawPath(path, rimShadowShPaint)
            restore()
            path.fillType = Path.FillType.EVEN_ODD
        }

        fun Canvas.drawBG(path: Path, color: Int, is3D: Boolean) {
            val shadowPaint = Paint().apply {
                this.color = color
            }
            val s = 108f / width
            if (shadowPaint.color.alpha < 100)
                shadowPaint.clearShadowLayer()
            else if (is3D)
                shadowPaint.setShadowLayer(8f / s, 0f, 3f / s, 0x55000000)
            else
                shadowPaint.setShadowLayer(21f / s, 0f, 4f / s, 0x22000000)
            if (is3D) {
                val rimOutBorderPaint = Paint().apply {
                    this.color = 0xbb000000.toInt()
                    xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
                    style = Paint.Style.STROKE
                    strokeWidth = height * 0.025f
                }
                drawPath(path, rimOutBorderPaint)
            }
            drawPath(path, shadowPaint)
        }
    }

    private val pic = Picture().record(content.bounds.width(), content.bounds.height()) {
        drawBG(path, color, is3D)
        clipPath(path)
        content.draw(this)
        if (is3D)
            drawRim(path)
    }

    override fun draw(canvas: Canvas) {
        val s = bounds.width() / content.bounds.width().toFloat()
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(s, s)
        pic.draw(canvas)
        canvas.restore()
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) { content.alpha = alpha }
    override fun getAlpha() = content.alpha
    override fun setColorFilter(cf: ColorFilter?) { content.colorFilter = cf }

    override fun getIntrinsicWidth() = content.intrinsicWidth
    override fun getIntrinsicHeight() = content.intrinsicHeight
    override fun getMinimumWidth() = content.minimumWidth
    override fun getMinimumHeight() = content.minimumHeight
}