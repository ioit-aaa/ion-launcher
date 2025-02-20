package one.zagura.IonLauncher.provider.icons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
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
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils.setGrayscale
import one.zagura.IonLauncher.util.drawable.ClippedDrawable
import one.zagura.IonLauncher.util.drawable.ReshapedAdaptiveIcon
import one.zagura.IonLauncher.util.drawable.ContactDrawable
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.drawable.IconGlossDrawable
import one.zagura.IonLauncher.util.drawable.NonDrawable
import kotlin.math.abs
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

    fun iconifyQuadImage(icon: Drawable): ClippedDrawable {
        val icon = if (doIconGloss) IconGlossDrawable(icon) else icon
        val w = icon.intrinsicWidth
        val h = icon.intrinsicHeight
        val path = Path().apply {
            val r = w * radiusRatio
            addRoundRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
        }
        icon.setBounds(0, 0, w, h)
        return ClippedDrawable(icon, path, iconBG, doIconRim)
    }

    fun makeContact(name: String): Drawable = ContactDrawable(
        name.substring(0, name.length.coerceAtMost(2)),
        radiusRatio, iconBG, iconFG)

    fun transformIconFromIconPack(icon: Drawable): Drawable =
        transformIcon(icon, willBeThemed = false, isIconPack = true)

    fun applyTheming(context: Context, icon: Drawable, iconPacks: List<IconPackInfo>): Drawable {
        val iconThemingInfo = iconPacks.firstNotNullOfOrNull {
            it.iconModificationInfo
        }
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
                return icon
            val w = icon.intrinsicWidth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                doMonochrome && !doMonochromeBG)
                return InsetDrawable(icon, w / 12)
            val isIconFullQuad = icon.toBitmap(1, 1)[0, 0].alpha == 255
            if (!doForceAdaptive && !isIconFullQuad)
                return icon
            return iconifyQuadImage(if (isIconFullQuad) icon else InsetDrawable(icon, w / 6))
        }

        var fg = icon.foreground?.let(::reshapeNestedAdaptiveIcons) ?: NonDrawable
        var bg = icon.background?.let(::reshapeNestedAdaptiveIcons)
        fg?.setGrayscale(doGrayscale)
        bg?.setGrayscale(doGrayscale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val monochrome = icon.monochrome
            if (doMonochrome) {
                if (monochrome != null) {
                    if (!doMonochromeBG) {
                        monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
                        val w = monochrome.intrinsicWidth
                        return InsetDrawable(monochrome, -w / if (willBeThemed) 5 else 7)
                    }
                    monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
                    fg = monochrome
                    return makeIcon(iconBG, fg, isIconPack)
                } else if (!doMonochromeBG) {
                    val i = makeIcon(bg, fg, isIconPack)
                    return InsetDrawable(i, fg.intrinsicWidth / 12)
                } else if (bg == null)
                    return makeIcon(iconBG, fg, isIconPack)
                else if (bg is ColorDrawable || bg is ShapeDrawable || bg is GradientDrawable) {
                    val pixel = bg.toBitmap(1, 1)[0, 0]
                    val lab = DoubleArray(3)
                    ColorUtils.colorToLAB(pixel, lab)
                    val bgL = lab[0]
                    ColorUtils.colorToLAB(iconBG, lab)
                    if (abs(bgL - lab[0]) < 30.0)
                        return makeIcon(iconBG, fg, isIconPack)
                }
            }
        }
        val color = when (bg) {
            is ShapeDrawable -> bg.paint.color
            is ColorDrawable -> bg.color
            is GradientDrawable -> bg.color?.defaultColor
                ?: return makeIcon(bg, fg, isIconPack)
            else -> return makeIcon(bg, fg, isIconPack)
        }
        return makeIcon(if (doGrayscale)
            ColorThemer.colorize(color, iconBG)
        else color, fg, isIconPack)
    }

    private fun makeIcon(
        bg: Drawable?,
        fg: Drawable?,
        isIconPack: Boolean,
    ): ClippedDrawable {
        val layers = LayerDrawable(arrayOf(if (doIconGloss && (doIconGlossOnThemed || !isIconPack))
            bg?.let(::IconGlossDrawable) else bg, fg))
        val w = layers.intrinsicWidth
        val h = layers.intrinsicHeight
        layers.setLayerInset(0, -w / 4, -h / 4, -w / 4, -h / 4)
        layers.setLayerInset(1, -w / 4, -h / 4, -w / 4, -h / 4)
        layers.setBounds(0, 0, w, h)

        val path = Path().apply {
            val r = w * radiusRatio
            addRoundRect(
                0f, 0f, w.toFloat(), w.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
        }
        return ClippedDrawable(layers, path, iconBG, doIconRim)
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

        val path = Path().apply {
            val r = w * radiusRatio
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
        var orig = Bitmap.createBitmap(
            icon.intrinsicWidth,
            icon.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(Canvas(orig))
        val scaledBitmap =
            Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        with(Canvas(scaledBitmap)) {
            val back = iconPackInfo.back
            if (back != null)
                drawBitmap(
                    back,
                    Rect(0, 0, back.width, back.height),
                    Rect(0, 0, iconSize, iconSize),
                    p)
            val scaledOrig =
                Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            with(Canvas(scaledOrig)) {
                val s = (iconSize * iconPackInfo.scaleFactor).toInt()
                val oldOrig = orig
                orig = Bitmap.createScaledBitmap(orig, s, s, true)
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
                Bitmap.createScaledBitmap(scaledOrig, iconSize, iconSize, true),
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
        BitmapDrawable(resources, scaledBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        icon
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reshapeNestedAdaptiveIcons(icon: Drawable): Drawable {
        return when (icon) {
            is AdaptiveIconDrawable -> {
                val w = max(icon.intrinsicWidth, icon.intrinsicHeight).coerceAtLeast(1)
                val path = Path().apply {
                    val r = w * radiusRatio
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
}