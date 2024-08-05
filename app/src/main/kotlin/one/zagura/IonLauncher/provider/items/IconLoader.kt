package one.zagura.IonLauncher.provider.items

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.ActionItem
import one.zagura.IonLauncher.data.items.OpenAlarmsItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.data.items.TorchToggleItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.drawable.ClippedDrawable
import one.zagura.IonLauncher.util.drawable.ContactDrawable
import one.zagura.IonLauncher.util.IconTheming
import one.zagura.IonLauncher.util.drawable.NonDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.drawable.IconGlossDrawable
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

object IconLoader {
    private val cacheApps = HashMap<App, Drawable>()
    private val cacheContacts = HashMap<String, Drawable>() // only use lookup key
    private val cacheShortcuts = HashMap<StaticShortcut, Drawable>()
    private var iconPacks = emptyList<IconTheming.IconPackInfo>()

    private val iconPacksLock = ReentrantLock()

    private var doGrayscale = false
    private var doMonochrome = false
    private var doMonochromeBG = false

    private var doIconRim = false
    private var doIconGloss = false

    private var radiusRatio = 0f

    private var iconFG = 0
    private var iconBG = 0

    fun updateIconPacks(context: Context, settings: Settings) {
        doGrayscale = settings["icon:grayscale", true]
        doMonochrome = settings["icon:monochrome", false]
        doMonochromeBG = settings["icon:monochrome-bg", true]
        doIconRim = settings["icon:rim", false]
        doIconGloss = settings["icon:gloss", false]
        radiusRatio = settings["icon:radius-ratio", 50] / 100f
        iconFG = ColorThemer.iconForeground(context)
        iconBG = ColorThemer.iconBackground(context)
        clearCache()
        TaskRunner.submit {
            iconPacksLock.withLock {
                iconPacks =
                    settings.getStrings("icon_packs").orEmpty().mapNotNull { iconPackPackage ->
                        try {
                            IconTheming.getIconPackInfo(context.packageManager, iconPackPackage)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
            }
        }
    }

    @SuppressLint("NewApi")
    fun loadIcon(context: Context, item: LauncherItem): Drawable = when (item) {
        is App -> loadIcon(context, item)
        is ContactItem -> loadIcon(context, item)
        is ActionItem -> loadIcon(context, item)
        is StaticShortcut -> loadIcon(context, item)
        is TorchToggleItem -> loadSymbolicIcon(context, R.drawable.ic_torch)
        is OpenAlarmsItem -> loadSymbolicIcon(context, R.drawable.ic_alarm)
    }

    fun loadIcon(context: Context, item: ActionItem): Drawable {
        val initIcon = context.packageManager.queryIntentActivities(Intent(item.action), 0)
            .firstOrNull()?.loadIcon(context.packageManager) ?: return NonDrawable
        return iconPacksLock.withLock {
            themeIcon(context, initIcon)
        }
    }

    fun loadIcon(context: Context, app: App): Drawable {
        return cacheApps.getOrPut(app) {
            iconPacksLock.withLock {
                val externalIcon = iconPacks.firstNotNullOfOrNull {
                    it.getDrawable(
                        app.packageName,
                        app.name,
                        context.resources.displayMetrics.densityDpi
                    )
                }
                if (externalIcon != null)
                    return@getOrPut externalIcon.apply {
                        setGrayscale(doGrayscale)
                    }
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                launcherApps.getActivityList(app.packageName, app.userHandle)
                    ?.find { it.name == app.name }
                    ?.getIcon(context.resources.displayMetrics.densityDpi)
                    ?.let { themeIcon(context, it) }
                    ?: return NonDrawable
            }
        }
    }

    fun loadIcon(context: Context, contact: ContactItem): Drawable {
        return cacheContacts.getOrPut(contact.lookupKey) {
            if (contact.iconUri != null) try {
                val inputStream = context.contentResolver.openInputStream(contact.iconUri)
                val pic = Drawable.createFromStream(inputStream, contact.iconUri.toString())
                    ?: NonDrawable
                pic.setBounds(0, 0, pic.intrinsicWidth, pic.intrinsicHeight)
                val path = Path().apply {
                    val w = pic.bounds.width()
                    val r = w * radiusRatio
                    addRoundRect(0f, 0f, w.toFloat(), w.toFloat(),
                        floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW)
                }
                return@getOrPut ClippedDrawable(pic, path, iconBG, doIconRim)
            } catch (_: FileNotFoundException) {}
            val realName = LabelLoader.loadLabel(context, contact).trim()
            if (realName.isEmpty())
                return@getOrPut NonDrawable
            ContactDrawable(
                realName.substring(0, realName.length.coerceAtMost(2)),
                radiusRatio, iconBG, iconFG,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun loadIcon(context: Context, shortcut: StaticShortcut): Drawable {
        return cacheShortcuts.getOrPut(shortcut) {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            if (!launcherApps.hasShortcutHostPermission())
                return NonDrawable
            val icon = launcherApps.getShortcutIconDrawable(
                shortcut.getShortcutInfo(context) ?: return@getOrPut NonDrawable,
                context.resources.displayMetrics.densityDpi
            ) ?: NonDrawable
            icon.setGrayscale(doGrayscale)
            icon
        }
    }

    fun clearCache() {
        cacheApps.clear()
        cacheContacts.clear()
        cacheShortcuts.clear()
    }

    fun removePackage(packageName: String) {
        val iterator = cacheApps.iterator()
        for ((app, _) in iterator)
            if (app.packageName == packageName)
                iterator.remove()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadSymbolicIcon(context: Context, res: Int): Drawable {
        val monochrome = context.getDrawable(res)!!
        if (!doMonochromeBG) {
            monochrome.setTint(iconFG)
            val w = monochrome.intrinsicWidth
            return InsetDrawable(monochrome, -w / 5)
        }
        monochrome.colorFilter = PorterDuffColorFilter(iconFG, PorterDuff.Mode.SRC_IN)
        return makeIcon(iconBG, monochrome)
    }

    private fun themeIcon(context: Context, icon: Drawable): Drawable {
        val iconThemingInfo = iconPacks.firstNotNullOfOrNull {
            if (it.iconModificationInfo.areUnthemedIconsChanged)
                it.iconModificationInfo.also { it.size = 128 } else null
        }
        val ti = transformIcon(icon, iconThemingInfo != null)
        return if (iconThemingInfo == null) ti
        else themeIcon(ti, iconThemingInfo, context.resources).apply {
            setGrayscale(doGrayscale)
        }
    }

    private fun transformIcon(icon: Drawable, willBeThemed: Boolean): Drawable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || icon !is AdaptiveIconDrawable) {
            icon.setGrayscale(doGrayscale)
            val w = icon.intrinsicWidth
            val h = icon.intrinsicHeight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                doMonochrome && !doMonochromeBG) {
                return InsetDrawable(icon, w / 12)
            }
            val path = Path().apply {
                val r = w * radiusRatio
                addRoundRect(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW
                )
            }
            var icon = if (icon.toBitmap(1, 1)[0, 0].alpha == 255) icon
                else InsetDrawable(icon, w / 6)
            if (doIconGloss)
                icon = IconGlossDrawable(icon)
            icon.setBounds(0, 0, w, h)
            return ClippedDrawable(icon, path, iconBG, doIconRim)
        }

        var fg = icon.foreground
        var bg = icon.background
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
                    return makeIcon(iconBG, fg)
                } else if (!doMonochromeBG) {
                    val i = makeIcon(bg, fg)
                    return InsetDrawable(i, fg.intrinsicWidth / 12)
                } else if (bg == null)
                    return makeIcon(iconBG, fg)
                else if (bg is ColorDrawable || bg is ShapeDrawable || bg is GradientDrawable) {
                    val pixel = bg.toBitmap(1, 1)[0, 0]
                    val lab = DoubleArray(3)
                    ColorUtils.colorToLAB(pixel, lab)
                    val bgL = lab[0]
                    ColorUtils.colorToLAB(iconBG, lab)
                    if (abs(bgL - lab[0]) < 30.0)
                        return makeIcon(iconBG, fg)
                }
            }
        }
        val color = when (bg) {
            is ShapeDrawable -> bg.paint.color
            is ColorDrawable -> bg.color
            is GradientDrawable -> bg.color?.defaultColor
                ?: return makeIcon(bg, fg)
            else -> return makeIcon(bg, fg)
        }
        return makeIcon(if (doGrayscale)
            ColorThemer.colorize(color, iconBG)
        else color, fg)
    }

    private fun makeIcon(
        bg: Drawable?,
        fg: Drawable?,
    ): ClippedDrawable {
        val layers = LayerDrawable(arrayOf(if (doIconGloss) bg?.let(::IconGlossDrawable) else bg, fg))
        val w = layers.intrinsicWidth
        val h = layers.intrinsicHeight
        layers.setLayerInset(0, -w / 6, -h / 6, -w / 6, -h / 6)
        layers.setLayerInset(1, -w / 6, -h / 6, -w / 6, -h / 6)
        layers.setBounds(0, 0, w, h)

        val path = Path().apply {
            val r = w * radiusRatio
            addRoundRect(
                0f, 0f, w.toFloat(), w.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW
            )
        }
        return ClippedDrawable(layers, path, iconBG, doIconRim)
    }

    private fun makeIcon(
        bg: Int,
        fg: Drawable,
    ): ClippedDrawable {
        if (doIconGloss)
            return makeIcon(FillDrawable(bg), fg)

        val w = fg.intrinsicWidth
        val h = fg.intrinsicHeight
        val layers = InsetDrawable(fg, -w / 6)
        layers.setBounds(0, 0, w, h)

        val path = Path().apply {
            val r = w * radiusRatio
            addRoundRect(
                0f, 0f, w.toFloat(), w.toFloat(),
                floatArrayOf(r, r, r, r, r, r, r, r), Path.Direction.CW
            )
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
    private fun themeIcon(
        icon: Drawable,
        iconPackInfo: IconTheming.IconGenerationInfo,
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
            Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
        Canvas(scaledBitmap).run {
            val back = iconPackInfo.back
            if (back != null) {
                drawBitmap(
                    back,
                    Rect(0, 0, back.width, back.height),
                    Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                    p
                )
            }
            val scaledOrig =
                Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
            Canvas(scaledOrig).run {
                val s = (iconPackInfo.size * iconPackInfo.scaleFactor).toInt()
                val oldOrig = orig
                orig = Bitmap.createScaledBitmap(orig, s, s, true)
                oldOrig.recycle()
                drawBitmap(
                    orig,
                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                    p
                )
                val mask = iconPackInfo.mask
                if (mask != null) {
                    drawBitmap(
                        mask,
                        Rect(0, 0, mask.width, mask.height),
                        Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                        maskP
                    )
                }
            }
            drawBitmap(
                Bitmap.createScaledBitmap(scaledOrig, iconPackInfo.size, iconPackInfo.size, true),
                0f,
                0f,
                p
            )
            val front = iconPackInfo.front
            if (front != null) {
                drawBitmap(
                    front,
                    Rect(0, 0, front.width, front.height),
                    Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                    p
                )
            }
            orig.recycle()
            scaledOrig.recycle()
        }
        BitmapDrawable(resources, scaledBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        icon
    }

    private fun Drawable.setGrayscale(value: Boolean) {
        colorFilter = if (value) ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
        }) else null
    }
}
