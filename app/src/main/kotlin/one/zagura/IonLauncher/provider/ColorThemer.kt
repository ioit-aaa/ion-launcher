package one.zagura.IonLauncher.provider

import android.content.Context
import androidx.core.graphics.ColorUtils
import one.zagura.IonLauncher.ui.ionApplication
import kotlin.math.abs
import kotlin.math.max

object ColorThemer {
    const val DEFAULT_DARK = 0x111111
    const val DEFAULT_LIGHT = 0xfefefe

    fun iconBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["icon:bg", DEFAULT_LIGHT] and 0xffffff or (s["icon:bg:alpha", 0xdd] shl 24)
    }
    fun iconForeground(context: Context): Int =
        context.ionApplication.settings["icon:fg", DEFAULT_DARK] or 0xff000000.toInt()


    fun cardBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["card:bg", DEFAULT_DARK] and 0xffffff or (s["card:bg:alpha", 0xdd] shl 24)
    }
    fun cardBackgroundOpaque(context: Context): Int =
        context.ionApplication.settings["card:bg", DEFAULT_DARK] or 0xff000000.toInt()
    fun cardForeground(context: Context): Int =
        context.ionApplication.settings["card:fg", DEFAULT_LIGHT] or 0xff000000.toInt()
    fun cardHint(context: Context): Int =
        context.ionApplication.settings["card:fg", DEFAULT_LIGHT] and 0xffffff or 0xaa000000.toInt()

    fun wallBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["wall:bg", DEFAULT_DARK] and 0xffffff or (s["wall:bg:alpha", 0xdd] shl 24)
    }
    fun wallForeground(context: Context): Int =
        context.ionApplication.settings["wall:fg", DEFAULT_LIGHT] or 0xff000000.toInt()

    fun drawerBackground(context: Context): Int {
        val s = context.ionApplication.settings
        return s["drawer:bg", DEFAULT_DARK] and 0xffffff or (s["drawer:bg:alpha", 0xdd] shl 24)
    }
    fun drawerBackgroundOpaque(context: Context): Int =
        context.ionApplication.settings["drawer:bg", DEFAULT_DARK] or 0xff000000.toInt()
    fun drawerForeground(context: Context): Int =
        context.ionApplication.settings["drawer:fg", DEFAULT_LIGHT] or 0xff000000.toInt()
    fun drawerHint(context: Context): Int =
        context.ionApplication.settings["drawer:fg", DEFAULT_LIGHT] and 0xffffff or 0xaa000000.toInt()
    fun drawerHighlight(context: Context): Int =
        context.ionApplication.settings["drawer:fg", DEFAULT_LIGHT] and 0xffffff or 0x33000000.toInt()

    fun level(color: Int, level: Double): Int {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        lab[0] = level * 100.0
        val ls = abs(lab[1]) + abs(lab[2])
        val f = (4.0 / ls).coerceAtMost(1.0) + level * 0.3
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
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color, lab)
        val l = lab[0]
        ColorUtils.colorToLAB(tint, lab)
        return ColorUtils.LABToColor(l, lab[1], lab[2])
    }
}