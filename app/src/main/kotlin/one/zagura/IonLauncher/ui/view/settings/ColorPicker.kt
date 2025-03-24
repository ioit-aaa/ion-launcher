package one.zagura.IonLauncher.ui.view.settings

import android.app.Dialog
import android.app.WallpaperManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.widget.doOnTextChanged
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.drawable.SquircleRectShape
import one.zagura.IonLauncher.util.drawable.UniformSquircleRectShape
import java.util.TreeSet
import kotlin.toString

class ColorPicker(
    context: Context,
    initColor: ColorThemer.ColorSetting,
    onSelect: (color: ColorThemer.ColorSetting) -> Unit,
) : Dialog(context) {

    private var selectedColor: ColorThemer.ColorSetting = ColorThemer.ColorSetting.Static(-1)
        set(value) {
            if (field == value)
                return
            field = value
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(value.get(context), hsl)
            hue.progress = hsl[0].toInt()
            saturation.progress = (hsl[1] * 300).toInt()
            lightness.progress = (hsl[2] * 300).toInt()
            val f = colorText.hasFocus()
            if (f) colorText.clearFocus()
            colorText.setText(formatColorString(ColorUtils.HSLToColor(hsl)))
            if (f) colorText.requestFocus()
        }

    val hue = SeekBar(context).apply {
        progressDrawable = generateSeekbarTrackDrawable(context)
        thumb = generateSeekbarThumbDrawable(context)
        splitTrack = false
        max = 360
        contentDescription = "H"
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = selectedColor.get(v.context)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[0] = progress.toFloat()
                selectedColor = ColorThemer.ColorSetting.Static(ColorUtils.HSLToColor(hsl))
            }
        })
    }
    val saturation = SeekBar(context).apply {
        progressDrawable = generateSeekbarTrackDrawable(context)
        thumb = generateSeekbarThumbDrawable(context)
        splitTrack = false
        max = 300
        contentDescription = "S"
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = selectedColor.get(v.context)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[1] = progress / 300f
                selectedColor = ColorThemer.ColorSetting.Static(ColorUtils.HSLToColor(hsl))
            }
        })
    }
    val lightness = SeekBar(context).apply {
        progressDrawable = generateSeekbarTrackDrawable(context)
        thumb = generateSeekbarThumbDrawable(context)
        splitTrack = false
        max = 300
        contentDescription = "L"
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = selectedColor.get(v.context)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[2] = progress / 300f
                selectedColor = ColorThemer.ColorSetting.Static(ColorUtils.HSLToColor(hsl))
            }
        })
    }
    val colorText = EditText(context).apply {
        val dp = resources.displayMetrics.density
        hint = formatColorString(0xffffff)
        setTextColor(ColorThemer.contrast(0, 0.9, 0))
        setHintTextColor(resources.getColor(R.color.color_hint))
        val p = (12 * dp).toInt()
        setPadding(p, p, p, p)
        setSingleLine()
        background = ShapeDrawable(UniformSquircleRectShape(10 * dp))
        doOnTextChanged { text, _, _, _ ->
            val c = parseColorString(text.toString())
            backgroundTintList = ColorStateList.valueOf(c ?: 0)
            setTextColor(ColorThemer.contrast(c ?: 0, 0.9, c ?: 0))
            if (c != null && hasFocus())
                selectedColor = ColorThemer.ColorSetting.Static(c)
        }
    }

    init {
        val dp = context.resources.displayMetrics.density
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
        }
        setContentView(
            content,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        val r = 26 * dp
        window!!.setBackgroundDrawable(ShapeDrawable(UniformSquircleRectShape(r)).apply {
            paint.color = context.resources.getColor(R.color.color_bg)
        })

        content.addView(TextView(context).apply {
            setText(R.string.dynamically_tint)
            setTextColor(resources.getColor(R.color.color_heading))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            isAllCaps = true
            setPadding(0, 0, 0, (6 * dp).toInt())
        })
        content.addView(GridLayout(context).apply {
            orientation = GridLayout.VERTICAL
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.SHADE)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.SHADE_LIGHTER)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.VIBRANT_LIGHT)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.VIBRANT_DARK)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.DARKER)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.DARK)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.LIGHT)
            addColorView(this@ColorPicker, ColorThemer.ColorSetting.Dynamic.LIGHTER)
        }, MarginLayoutParams((320 * dp).toInt(), (128 * dp).toInt()).apply {
            bottomMargin = (12 * dp).toInt()
        })
        content.addView(TextView(context).apply {
            setText(R.string.suggestions)
            setTextColor(resources.getColor(R.color.color_heading))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            isAllCaps = true
            setPadding(0, 0, 0, (6 * dp).toInt())
        })
        val suggestions = loadColorSuggestions(context)
        content.addView(GridLayout(context).apply {
            orientation = GridLayout.VERTICAL
            for (color in suggestions)
                addColorView(this@ColorPicker, ColorThemer.ColorSetting.Static(color))
        }, MarginLayoutParams((320 * dp).toInt(), (((suggestions.size + 4) / 5) * 64 * dp).toInt()).apply {
            bottomMargin = (12 * dp).toInt()
        })
        content.addView(
            colorText,
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * dp).toInt()
            })
        val l = LayoutParams(LayoutParams.MATCH_PARENT, (42 * dp).toInt())
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val v = (4 * dp).toInt()
            setPadding(0, v, 0, v)
            addView(TextView(context).apply {
                setTextColor(resources.getColor(R.color.color_hint))
                typeface = Typeface.DEFAULT_BOLD
                text = "L"
            })
            addView(lightness, l)
        }, l)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val v = (4 * dp).toInt()
            setPadding(0, v, 0, v)
            addView(TextView(context).apply {
                setTextColor(resources.getColor(R.color.color_hint))
                typeface = Typeface.DEFAULT_BOLD
                text = "S"
            })
            addView(saturation, l)
        }, l)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val v = (4 * dp).toInt()
            setPadding(0, v, 0, v)
            addView(TextView(context).apply {
                setTextColor(resources.getColor(R.color.color_hint))
                typeface = Typeface.DEFAULT_BOLD
                text = "H"
            })
            addView(hue, l)
        }, l)
        content.addView(makeButtons(context, this) {
            onSelect(selectedColor)
        }, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (8 * dp).toInt()
        })
        selectedColor = initColor
    }

    companion object {
        fun show(
            context: Context,
            initColor: ColorThemer.ColorSetting,
            onSelect: (color: ColorThemer.ColorSetting) -> Unit
        ) = ColorPicker(context, initColor, onSelect).show()

        private fun loadColorSuggestions(context: Context): TreeSet<Int> {
            val set = arrayOf(
                ColorThemer.wallBackground(context) and 0xffffff,
                ColorThemer.wallForeground(context) and 0xffffff,
                ColorThemer.drawerBackground(context) and 0xffffff,
                ColorThemer.drawerForeground(context) and 0xffffff,
                ColorThemer.iconBackground(context) and 0xffffff,
                ColorThemer.iconForeground(context) and 0xffffff,
                ColorThemer.cardBackground(context) and 0xffffff,
                ColorThemer.cardForeground(context) and 0xffffff,
            ).toCollection(TreeSet { a, b ->
                if (a == b)
                    return@TreeSet 0
                val aa = FloatArray(3)
                ColorUtils.colorToHSL(a, aa)
                val bb = FloatArray(3)
                ColorUtils.colorToHSL(b, bb)
                if (aa[0] == bb[0]) {
                    if (aa[2] < bb[2]) -1 else 1
                } else if (aa[0] < bb[0]) -1 else 1
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                set.add(context.getColor(android.R.color.system_accent1_500) and 0xffffff)
                set.add(context.getColor(android.R.color.system_accent2_500) and 0xffffff)
                set.add(context.getColor(android.R.color.system_accent3_500) and 0xffffff)
                set.add(context.getColor(android.R.color.system_neutral1_900) and 0xffffff)
                set.add(context.getColor(android.R.color.system_neutral2_900) and 0xffffff)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val wallColors = WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (wallColors != null) {
                    set.add(wallColors.primaryColor.toArgb() and 0xffffff)
                    val s = wallColors.secondaryColor
                    val t = wallColors.tertiaryColor
                    if (s != null)
                        set.add(s.toArgb() and 0xffffff)
                    if (t != null)
                        set.add(t.toArgb() and 0xffffff)
                }
            }
            return set
        }

        fun formatColorString(@ColorInt color: Int): String =
            "#${(color and 0xffffff).toString(16).padStart(6, '0')}"

        private fun parseColorString(color: String): Int? =
            try { color.toColorInt() } catch (_: Exception) { null }

        private fun ViewGroup.addColorView(colorPicker: ColorPicker, color: ColorThemer.ColorSetting) {
            if (tag == null)
                tag = 0
            val dp = resources.displayMetrics.density
            val r = tag as Int / 5
            val c = tag as Int % 5
            addView(View(context).apply {
                val lra = 6 * dp
                val bra = 6 * dp
                val tl = if (r == 0 && c == 0) bra else lra
                val tr = if (r == 0 && c == 4) bra else lra
                val realColor = color.get(context)
                val highlight = DoubleArray(3)
                    .also { ColorUtils.colorToLAB(realColor, it) }
                    .also {
                        it[0] = (it[0] + 20.0) * 1.2
                        it[1] *= 1.6
                        it[1] *= 1.6
                    }
                    .let { ColorUtils.LABToColor(it[0], it[1], it[2]) }
                background = RippleDrawable(
                    ColorStateList.valueOf(highlight),
                    ShapeDrawable(SquircleRectShape(tl, tr, lra, lra)), null
                )
                backgroundTintList = ColorStateList.valueOf(realColor or 0xff000000.toInt())
                setOnClickListener {
                    colorPicker.selectedColor = color
                }
                contentDescription = if (color is ColorThemer.ColorSetting.Dynamic)
                    color.toString()
                else formatColorString(realColor)
            }, GridLayout.LayoutParams().apply {
                val m = (6 * dp).toInt()
                width = 0
                height = 0
                rowSpec = GridLayout.spec(r, 1, 1f)
                columnSpec = GridLayout.spec(c, 1, 1f)
                if (c != 0) leftMargin = m
                if (r != 0) topMargin = m
                tag = tag as Int + 1
            })
        }

        private fun makeButtons(context: Context, w: Dialog, onOk: (View) -> Unit) =
            LinearLayout(context).apply {
                val dp = resources.displayMetrics.density
                val br = 10 * dp
                val h = (32 * dp).toInt()
                val v = (15 * dp).toInt()
                orientation = LinearLayout.HORIZONTAL
                addView(TextView(context).apply {
                    setText(android.R.string.ok)
                    setTextColor(resources.getColor(R.color.color_button_text))
                    background = RippleDrawable(
                        ColorStateList.valueOf(resources.getColor(R.color.color_hint)),
                        ShapeDrawable(SquircleRectShape(br, 0f, 0f, br)).apply {
                            paint.color = resources.getColor(R.color.color_button)
                        }, null
                    )
                    setPadding(h, v, h, v)
                    gravity = Gravity.CENTER_HORIZONTAL
                    typeface = Typeface.DEFAULT_BOLD
                    setSingleLine()
                    isAllCaps = true
                    setOnClickListener {
                        onOk(it)
                        w.dismiss()
                    }
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (2 * dp).toInt()
                })
                addView(TextView(context).apply {
                    setText(android.R.string.cancel)
                    setTextColor(resources.getColor(R.color.color_button_text))
                    background = RippleDrawable(
                        ColorStateList.valueOf(resources.getColor(R.color.color_hint)),
                        ShapeDrawable(SquircleRectShape(0f, br, br, 0f)).apply {
                            paint.color = resources.getColor(R.color.color_button)
                        }, null
                    )
                    setPadding(h, v, h, v)
                    gravity = Gravity.CENTER_HORIZONTAL
                    typeface = Typeface.DEFAULT_BOLD
                    setSingleLine()
                    isAllCaps = true
                    setOnClickListener { w.dismiss() }
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            }
    }
}