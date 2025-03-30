package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.graphics.alpha
import androidx.core.graphics.toXfermode
import androidx.core.graphics.translationMatrix
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.drawable.SquircleRectShape

class SharedDrawingContext(context: Context) {

    val tmpRect = Rect()

    var radius = 0f
        private set

    var iconSize = 0f
        private set

    private var doSkeumorphism = false
    private var doSquircle = true

    val cardPaint = Paint()
    val cardBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
    }

    val titlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val subtitlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
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

    fun drawCard(dp: Float, canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, r: Float = radius) {
        if (doSquircle)
            canvas.drawPath(SquircleRectShape.createPath(x1 - x0, y1 - y0, r, r, r, r).apply {
                transform(translationMatrix(x0, y0))
            }, cardPaint)
        else
            canvas.drawRoundRect(x0, y0, x1, y1, r, r, cardPaint)

        if (!doSkeumorphism)
            return

        val br = r + dp / 2
        if (doSquircle)
            canvas.drawPath(SquircleRectShape.createPath(x1 - x0 + dp, y1 - y0 + dp, br, br, br, br).apply {
                transform(translationMatrix(x0 - dp / 2, y0 - dp / 2))
            }, cardBorderPaint)
        else
            canvas.drawRoundRect(x0 - dp / 2, y0 - dp / 2, x1 + dp / 2, y1 + dp / 2, br, br, cardBorderPaint)

        val path = if (doSquircle)
            SquircleRectShape.createPath(x1 - x0, y1 - y0, r, r, r, r).apply {
                transform(translationMatrix(x0, y0))
            }
        else Path().apply {
            addRoundRect(x0, y0, x1, y1, r, r, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)

        val rimPaint = Paint().apply {
            color = 0x77ffffff
            xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
        }
        val rimShadowPaint = Paint().apply {
            color = 0x22ffffff
            xfermode = PorterDuff.Mode.OVERLAY.toXfermode()
        }
        val rimBorderPaint = Paint().apply {
            color = 0x11eeeeee
            style = Paint.Style.STROKE
            strokeWidth = 2 * dp
        }

        canvas.drawPath(path, rimBorderPaint)

        path.fillType = Path.FillType.INVERSE_EVEN_ODD
        canvas.save()
        canvas.translate(0f, dp / 2f)
        canvas.drawPath(path, rimPaint)
        canvas.translate(0f, -dp)
        canvas.drawPath(path, rimShadowPaint)
        canvas.restore()
        path.fillType = Path.FillType.EVEN_ODD

        canvas.restore()
    }

    fun applyLayoutCustomizations(context: Context, settings: Settings) {
        val dp = context.resources.displayMetrics.density
        iconSize = settings["dock:icon-size", 48] * dp
        radius = iconSize * settings["icon:radius-ratio", 25] / 100f
        doSkeumorphism = settings["card:skeumorph", false]
        doSquircle = settings["icon:squircle", true]
    }
    fun applyColorCustomizations(context: Context) {
        val dp = context.resources.displayMetrics.density
        cardPaint.color = ColorThemer.cardBackground(context)
        val c = ColorThemer.cardForeground(context)
        titlePaint.color = c
        textPaint.color = c
        subtitlePaint.color = ColorThemer.cardHint(context)

        if (doSkeumorphism) {
            cardBorderPaint.color = (0x66 + 0x44 * (cardPaint.color.alpha / 255f)).toInt() shl 24
            cardBorderPaint.strokeWidth = 1 * dp
            cardPaint.setShadowLayer(
                10f,
                0f,
                5f,
                (0x11 + 0x44 * (cardPaint.color.alpha / 255f)).toInt() shl 24
            )
        } else if (cardPaint.color.alpha != 255 || ColorThemer.lightness(cardPaint.color) - ColorThemer.lightness(ColorThemer.wallBackground(context)) <= 0.1)
            cardPaint.clearShadowLayer()
        else
            cardPaint.setShadowLayer(21f, 0f, 0f, 0x22000000)
    }
}