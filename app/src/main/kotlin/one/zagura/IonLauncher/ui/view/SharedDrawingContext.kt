package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.Settings

class SharedDrawingContext(context: Context) {

    val tmpRect = Rect()

    var radius = 0f
        private set

    var iconSize = 0f
        private set

    val pillPaint = Paint()

    val titlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val subtitlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }
    val textPaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }

    val textHeight = run {
        textPaint.getTextBounds("X", 0, 1, tmpRect)
        tmpRect.height()
    }

    fun applyCustomizations(context: Context, settings: Settings) {
        val dp = context.resources.displayMetrics.density
        radius = settings["dock:icon-size", 48] * settings["icon:radius-ratio", 50] * dp / 100f
        iconSize = settings["dock:icon-size", 48] * dp
        pillPaint.color = ColorThemer.iconBackground(context)
        val c = ColorThemer.iconForeground(context)
        titlePaint.color = c
        textPaint.color = c
        subtitlePaint.color = ColorThemer.iconHint(context)
    }
}