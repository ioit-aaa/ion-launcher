package one.zagura.IonLauncher.provider

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import one.zagura.IonLauncher.ui.ionApplication
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object ColorThemer {
    fun iconBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["icon:bg", ColorSetting.Dynamic.LIGHT].get(context) and 0xffffff or (s["icon:bg:alpha", 0xdd] shl 24)
    }
    fun iconForeground(context: Context): Int =
        context.ionApplication.settings["icon:fg", ColorSetting.Dynamic.SHADE].get(context) or 0xff000000.toInt()


    fun cardBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["card:bg", ColorSetting.Dynamic.SHADE_LIGHTER].get(context) and 0xffffff or (s["card:bg:alpha", 0xdd] shl 24)
    }
    fun cardBackgroundOpaque(context: Context): Int =
        context.ionApplication.settings["card:bg", ColorSetting.Dynamic.SHADE_LIGHTER].get(context) or 0xff000000.toInt()
    fun cardForeground(context: Context): Int =
        context.ionApplication.settings["card:fg", ColorSetting.Dynamic.LIGHTER].get(context) or 0xff000000.toInt()
    fun cardHint(context: Context): Int =
        context.ionApplication.settings["card:fg", ColorSetting.Dynamic.LIGHTER].get(context) and 0xffffff or 0xaa000000.toInt()

    fun wallBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["wall:bg", ColorSetting.Dynamic.SHADE].get(context) and 0xffffff or (s["wall:bg:alpha", 0x33] shl 24)
    }
    fun wallForeground(context: Context): Int =
        context.ionApplication.settings["wall:fg", ColorSetting.Dynamic.LIGHT].get(context) or 0xff000000.toInt()

    fun drawerBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["drawer:bg", ColorSetting.Dynamic.SHADE].get(context) and 0xffffff or (s["drawer:bg:alpha", 0xff] shl 24)
    }
    fun drawerBackgroundOpaque(context: Context): Int =
        context.ionApplication.settings["drawer:bg", ColorSetting.Dynamic.SHADE].get(context) or 0xff000000.toInt()
    fun drawerForeground(context: Context): Int =
        context.ionApplication.settings["drawer:fg", ColorSetting.Dynamic.LIGHT].get(context) or 0xff000000.toInt()
    fun drawerHint(context: Context): Int =
        context.ionApplication.settings["drawer:fg", ColorSetting.Dynamic.LIGHT].get(context) and 0xffffff or 0xaa000000.toInt()
    fun drawerHighlight(context: Context): Int =
        context.ionApplication.settings["drawer:fg", ColorSetting.Dynamic.LIGHT].get(context) and 0xffffff or 0x33000000.toInt()

    fun level(color: Int, level: Double): Int {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        lab[0] = level * 100.0
        val ls = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
        val f = (20.0 / ls).coerceAtMost(1.0) + level * 0.3
        lab[1] *= f
        lab[2] *= f
        return ColorUtils.LABToColor(lab[0], lab[1], lab[2])
    }

    fun contrast(color: Int, level: Double, against: Int): Int {
        val againstLab = DoubleArray(3)
        ColorUtils.colorToLAB(against, againstLab)
        return level(color, if (againstLab[0] > 50.0)
            (1.0 - level) * 0.5 else level * 0.5 + 0.5)
    }

    fun saturate(color: Int): Int {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        val f = 70.0 / max(abs(lab[1]), abs(lab[2]))
        lab[0] = 70.0
        lab[1] *= f
        lab[2] *= f
        return ColorUtils.LABToColor(lab[0], lab[1], lab[2])
    }

    fun lightness(color: Int): Double {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        return lab[0] / 100f
    }

    fun colorize(color: Int, tint: Int): Int {
        val a = color.alpha * tint.alpha / 255
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        val l = lab[0]
        val s = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
        ColorUtils.colorToLAB(tint, lab)
        val ts = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
        lab[1] *= (s + ts) / 2 / ts
        lab[2] *= (s + ts) / 2 / ts
        return ColorUtils.LABToColor(l, lab[1], lab[2]) and 0xffffff or (a shl 24)
    }

    sealed interface ColorSetting {
        fun get(context: Context): Int

        enum class Dynamic : ColorSetting {
            SHADE, SHADE_LIGHTER, VIBRANT_LIGHT, VIBRANT_DARK, DARKER, DARK, LIGHT, LIGHTER;
            override fun get(context: Context): Int {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    when (this) {
                        DARKER -> return context.resources.getColor(android.R.color.system_neutral1_900)
                        DARK -> return context.resources.getColor(android.R.color.system_neutral1_800)
                        LIGHT -> return context.resources.getColor(android.R.color.system_neutral1_100)
                        LIGHTER -> return context.resources.getColor(android.R.color.system_neutral1_10)
                        else -> {}
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    val w = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
                    val c = w.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    if (c != null) {
                        return when (this) {
                            SHADE -> level(c.primaryColor.toArgb(), 0.1)
                            SHADE_LIGHTER -> level(c.primaryColor.toArgb(), 0.2)
                            VIBRANT_LIGHT, VIBRANT_DARK -> maxOf(c.primaryColor.toArgb(), c.secondaryColor?.toArgb() ?: 0, c.tertiaryColor?.toArgb() ?: 0) { a, b ->
                                val lab = DoubleArray(3)
                                ColorUtils.colorToLAB(a, lab)
                                val sa = lab[1] * lab[1] + lab[2] * lab[2]
                                ColorUtils.colorToLAB(b, lab)
                                val sb = lab[1] * lab[1] + lab[2] * lab[2]
                                sa.compareTo(sb)
                            }.let {
                                if (this == VIBRANT_DARK) {
                                    val lab = DoubleArray(3)
                                    ColorUtils.colorToLAB(it, lab)
                                    ColorUtils.LABToColor(lab[0].coerceAtMost(50.0), lab[1], lab[2])
                                } else saturate(it)
                            }
                            LIGHTER, LIGHT -> maxOf(c.primaryColor.toArgb(), c.secondaryColor?.toArgb() ?: 0, c.tertiaryColor?.toArgb() ?: 0) { a, b ->
                                val lab = DoubleArray(3)
                                ColorUtils.colorToLAB(a, lab)
                                val sa = lab[0]
                                ColorUtils.colorToLAB(b, lab)
                                val sb = lab[0]
                                sa.compareTo(sb)
                            }
                            DARKER, DARK -> minOf(c.primaryColor.toArgb(), c.secondaryColor?.toArgb() ?: 0xffffffff.toInt(), c.tertiaryColor?.toArgb() ?: 0xffffffff.toInt()) { a, b ->
                                val lab = DoubleArray(3)
                                ColorUtils.colorToLAB(a, lab)
                                val sa = lab[0]
                                ColorUtils.colorToLAB(b, lab)
                                val sb = lab[0]
                                sa.compareTo(sb)
                            }
                        }
                    }
                }
                return when (this) {
                    SHADE, SHADE_LIGHTER -> 0x111111
                    VIBRANT_LIGHT, VIBRANT_DARK -> 0x888888
                    LIGHT -> 0xe2e2e2
                    LIGHTER -> 0xfefefe
                    DARK -> 0x222222
                    DARKER -> 0x111111
                }
            }
        }
        @JvmInline
        value class Static(val value: Int) : ColorSetting {
            override fun get(context: Context) = value
        }
    }
}