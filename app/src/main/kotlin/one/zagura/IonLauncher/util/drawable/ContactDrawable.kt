package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable

internal class ContactDrawable(
    private val text: String,
    private val radiusRatio: Float,
    bgColor: Int,
    fgColor: Int,
) : Drawable() {

    private val textPaint = Paint().apply {
        color = fgColor
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.create("serif", Typeface.BOLD)
    }

    private val bgPaint = Paint().apply {
        this.color = bgColor
        isAntiAlias = true
        setShadowLayer(21f, 0f, 4f, 0x22000000)
    }

    override fun draw(canvas: Canvas) {
        textPaint.textSize = bounds.height() / 2.5f
        val r = bounds.width() * radiusRatio
        canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), r, r, bgPaint)
        val x = bounds.left + bounds.width() / 2f
        val y = bounds.top + (bounds.height() - (textPaint.descent() + textPaint.ascent())) / 2f
        canvas.drawText(text, x, y, textPaint)
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getIntrinsicWidth() = 128
    override fun getIntrinsicHeight() = 128
    override fun getMinimumWidth() = 128
    override fun getMinimumHeight() = 128
}