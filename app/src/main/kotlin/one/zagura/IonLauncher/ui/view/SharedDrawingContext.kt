package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.TypedValue
import one.zagura.IonLauncher.util.Settings

class SharedDrawingContext {

    val tmpRect = Rect()

    var radius = 0f
        private set

    var iconSize = 0f
        private set

    var textHeight = 0
        private set

    fun applyCustomizations(context: Context, settings: Settings) {
        val dp = context.resources.displayMetrics.density
        radius = settings["dock:icon-size", 48] * settings["icon:radius-ratio", 50] * dp / 100f
        iconSize = settings["dock:icon-size", 48] * dp
        textHeight = run {
            val textPaint = TextPaint().apply {
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
                isSubpixelText = true
            }
            textPaint.getTextBounds("X", 0, 1, tmpRect)
            tmpRect.height()
        }
    }
}