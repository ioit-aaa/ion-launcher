package one.zagura.IonLauncher.provider.icons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.get
import androidx.core.graphics.scale
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils.setGrayscale
import one.zagura.IonLauncher.util.drawable.ClippedDrawable
import one.zagura.IonLauncher.util.drawable.ReshapedAdaptiveIcon
import one.zagura.IonLauncher.util.drawable.ContactDrawable
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.drawable.IconGlossDrawable
import one.zagura.IonLauncher.util.drawable.NonDrawable
import one.zagura.IonLauncher.util.drawable.SquircleRectShape
import kotlin.math.max

object IconThemer {

    private var doForceAdaptive = false

    var doGrayscale = false
        private set
    private var doMonochrome = false
    private var doMonochromeBG = false

    private var doIconRim = false
    private var doIconGloss = false
    private var doIconGlossOnThemed = false

    private var radiusRatio = 0f
    private var doSquircle = true

    private var iconFG = 0
    private var iconBG = 0

    private var iconSize = 0

    fun updateSettings(context: Context, settings: Settings) {
        doForceAdaptive = settings["icon:reshape-legacy", true]
        doGrayscale = settings["icon:grayscale", false]
        doMonochrome = settings["icon:monochrome", false]
        doMonochromeBG = settings["icon:monochrome-bg", true]
        doIconRim = settings["icon:rim", false]
        doIconGloss = settings["icon:gloss", false]
        doIconGlossOnThemed = settings["icon:gloss-themed", false]
        radiusRatio = settings["icon:radius-ratio", 25] / 100f
        doSquircle = settings["icon:squircle", true]
        iconFG = ColorThemer.iconForeground(context)
        iconBG = ColorThemer.iconBackground(context)
        val dp = context.resources.displayMetrics.density
        iconSize = (settings["dock:icon-size", 48] * dp).toInt()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadSymbolicIcon(context: Context, res: Int): Drawable {
        val monochrome = context.getDrawable(res)!!
        if (!doMonochromeBG) {
            monochrome.setTint(iconFG)
            val w = monochrome.intrinsicWidth
            return InsetDrawable(monochrome, -w / 5)
        }
        monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
        return makeIcon(iconBG, monochrome, false)
    }

    fun fromQuadImage(initIcon: Drawable): Drawable {
        var icon = if (doIconGloss) IconGlossDrawable(initIcon, radiusRatio) else initIcon
        if (initIcon is BitmapDrawable && max(icon.intrinsicWidth, icon.intrinsicHeight) >= iconSize)
            icon = BitmapDrawable(null, icon.toBitmap(iconSize, iconSize))
        val w = icon.intrinsicWidth
        val h = icon.intrinsicHeight
        val r = w * radiusRatio
        val path = if (doSquircle)
            SquircleRectShape.createPath(w.toFloat(), h.toFloat(), r, r, r, r)
        else Path().apply {
            addRoundRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
        }
        icon.setBounds(0, 0, w, h)
        return ClippedDrawable(icon, path, iconBG, doIconRim)
    }

    fun fromContactName(name: String): Drawable = ContactDrawable(
        name.substring(0, name.length.coerceAtMost(2)),
        radiusRatio, iconBG, iconFG)

    fun transformIconFromIconPack(icon: Drawable): Drawable =
        transformIcon(icon, willBeThemed = false, isIconPack = true)

    fun applyTheming(context: Context, icon: Drawable, iconThemingInfo: IconPackInfo.IconGenInfo?): Drawable {
        val ti = transformIcon(icon, iconThemingInfo != null, false)
        return if (iconThemingInfo == null) ti
        else applyIconPackTheming(ti, iconThemingInfo, context.resources).apply {
            setGrayscale(doGrayscale)
        }
    }

    private fun transformIcon(icon: Drawable, willBeThemed: Boolean, isIconPack: Boolean): Drawable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || icon !is AdaptiveIconDrawable) {
            icon.setGrayscale(doGrayscale)
            if (isIconPack)
                return if (icon is BitmapDrawable)
                    rescaleIconIfBig(icon)
                else icon
            val w = icon.intrinsicWidth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                doMonochrome && !doMonochromeBG)
                return InsetDrawable(icon, w / 12)
            val isIconFullQuad = icon.toBitmap(1, 1)[0, 0].alpha == 255
            if (!doForceAdaptive && !isIconFullQuad)
                return icon
            return fromQuadImage(if (isIconFullQuad) icon else InsetDrawable(icon, w / 6))
        }

        if (doMonochrome && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val monochrome = icon.monochrome
            if (monochrome != null) {
                if (!doMonochromeBG) {
                    monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
                    val w = monochrome.intrinsicWidth
                    return InsetDrawable(monochrome, -w / if (willBeThemed) 5 else 7)
                }
                monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
                return makeIcon(iconBG, monochrome, isIconPack)
            }
        }

        val bg = icon.background?.let(::reshapeNestedAdaptiveIcons)
        val fg = icon.foreground?.let(::reshapeNestedAdaptiveIcons) ?: NonDrawable
        fg.setGrayscale(doGrayscale)

        var icon: Drawable = run {
            if (doGrayscale) when (bg) {
                null -> return@run makeIcon(iconBG, fg, isIconPack)
                is ColorDrawable ->
                    return@run makeIcon(ColorThemer.colorize(bg.color, iconBG), fg, isIconPack)
                is ShapeDrawable ->
                    return@run makeIcon(ColorThemer.colorize(bg.paint.color, iconBG), fg, isIconPack)
                is GradientDrawable -> {
                    bg.color?.let {
                        return@run makeIcon(ColorThemer.colorize(it.defaultColor, iconBG), fg, isIconPack)
                    }
                    bg.colors = bg.colors?.map { ColorThemer.colorize(it, iconBG) }?.toIntArray()
                }
                else -> bg.setGrayscale(true)
            }
            when (bg) {
                is ShapeDrawable -> makeIcon(bg.paint.color, fg, isIconPack)
                is ColorDrawable -> makeIcon(bg.color, fg, isIconPack)
                else -> makeIcon(bg, fg, isIconPack)
            }
        }
        if (doMonochrome && !doMonochromeBG)
            icon = InsetDrawable(icon, fg.intrinsicWidth / 12)
        return icon
    }

    private fun makeIcon(
        bg: Drawable?,
        fg: Drawable?,
        isIconPack: Boolean,
    ): ClippedDrawable {
        val layers = LayerDrawable(arrayOf(bg, fg))
        var w = layers.intrinsicWidth
        var h = layers.intrinsicHeight
        layers.setLayerInset(0, -w / 4, -h / 4, -w / 4, -h / 4)
        layers.setLayerInset(1, -w / 4, -h / 4, -w / 4, -h / 4)

        val glossed = if (doIconGloss && (doIconGlossOnThemed || !isIconPack))
            IconGlossDrawable(layers, radiusRatio) else layers

        val final = if ((bg is BitmapDrawable && max(bg.bitmap.width, bg.bitmap.height) > iconSize) ||
            (fg is BitmapDrawable && max(fg.bitmap.width, fg.bitmap.height) > iconSize))
            BitmapDrawable(null, glossed.toBitmap(w, h).scale(iconSize, iconSize)).also {
                w = iconSize
                h = iconSize
            }
        else glossed

        val r = w * radiusRatio
        val path = if (doSquircle)
            SquircleRectShape.createPath(w.toFloat(), h.toFloat(), r, r, r, r)
        else Path().apply {
            addRoundRect(
                0f, 0f, w.toFloat(), w.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
        }

        final.setBounds(0, 0, w, h)
        return ClippedDrawable(final, path, iconBG, doIconRim)
    }

    private fun makeIcon(
        bg: Int,
        fg: Drawable,
        isIconPack: Boolean,
    ): ClippedDrawable {
        if (doIconGloss && (doIconGlossOnThemed || !isIconPack))
            return makeIcon(FillDrawable(bg), fg, false)

        val w = fg.intrinsicWidth
        val h = fg.intrinsicHeight
        val layers = InsetDrawable(fg, -w / 4)
        layers.setBounds(0, 0, w, h)

        val r = w * radiusRatio
        val path = if (doSquircle)
            SquircleRectShape.createPath(w.toFloat(), h.toFloat(), r, r, r, r)
        else Path().apply {
            addRoundRect(
                0f, 0f, w.toFloat(), w.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
        }
        return ClippedDrawable(layers, path, bg, doIconRim)
    }

    private val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
    }
    private val maskP = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private fun applyIconPackTheming(
        icon: Drawable,
        iconPackInfo: IconPackInfo.IconGenInfo,
        resources: Resources
    ): Drawable = try {
        var orig = createBitmap(icon.intrinsicWidth, icon.intrinsicHeight)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(Canvas(orig))
        val scaledBitmap = createBitmap(iconSize, iconSize)
        with(Canvas(scaledBitmap)) {
            val back = iconPackInfo.back
            if (back != null)
                drawBitmap(
                    back,
                    Rect(0, 0, back.width, back.height),
                    Rect(0, 0, iconSize, iconSize),
                    p)
            val scaledOrig = createBitmap(iconSize, iconSize)
            with(Canvas(scaledOrig)) {
                val s = (iconSize * iconPackInfo.scaleFactor).toInt()
                val oldOrig = orig
                orig = orig.scale(s, s)
                oldOrig.recycle()
                drawBitmap(
                    orig,
                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                    p)
                val mask = iconPackInfo.mask
                if (mask != null)
                    drawBitmap(
                        mask,
                        Rect(0, 0, mask.width, mask.height),
                        Rect(0, 0, iconSize, iconSize),
                        maskP)
            }
            drawBitmap(
                scaledOrig.scale(iconSize, iconSize),
                0f,
                0f,
                p)
            val front = iconPackInfo.front
            if (front != null)
                drawBitmap(
                    front,
                    Rect(0, 0, front.width, front.height),
                    Rect(0, 0, iconSize, iconSize),
                    p)
            orig.recycle()
            scaledOrig.recycle()
        }
        scaledBitmap.toDrawable(resources)
    } catch (e: Exception) {
        e.printStackTrace()
        icon
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reshapeNestedAdaptiveIcons(icon: Drawable): Drawable {
        return when (icon) {
            is AdaptiveIconDrawable -> {
                val w = max(icon.intrinsicWidth, icon.intrinsicHeight).coerceAtLeast(1)
                val r = w * radiusRatio
                val path = if (doSquircle)
                    SquircleRectShape.createPath(w.toFloat(), w.toFloat(), r, r, r, r)
                else Path().apply {
                    addRoundRect(
                        0f, 0f, w.toFloat(), w.toFloat(),
                        floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
                }
                ReshapedAdaptiveIcon(w,
                    icon.background?.let(::reshapeNestedAdaptiveIcons) ?: NonDrawable,
                    icon.foreground?.let(::reshapeNestedAdaptiveIcons) ?: NonDrawable, path)
            }
            is InsetDrawable -> icon.apply {
                drawable = reshapeNestedAdaptiveIcons(icon.drawable ?: return icon)
            }
            is LayerDrawable -> icon.apply {
                for (i in 0 until icon.numberOfLayers) {
                    val l = getDrawable(i) ?: continue
                    setDrawable(i, reshapeNestedAdaptiveIcons(l))
                }
            }
            else -> icon
        }
    }

    private fun rescaleIconIfBig(icon: BitmapDrawable): BitmapDrawable {
        val b = icon.bitmap ?: return icon
        val s = max(b.width, b.height)
        if (s <= iconSize)
            return icon
        return BitmapDrawable(b.scale(b.width * iconSize / s, b.height * iconSize / s))
    }
}