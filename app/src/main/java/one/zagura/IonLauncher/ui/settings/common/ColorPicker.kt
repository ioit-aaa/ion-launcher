package one.zagura.IonLauncher.ui.settings.common

import android.app.Dialog
import android.app.WallpaperManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
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
import one.zagura.IonLauncher.provider.ColorThemer

object ColorPicker {

    fun show(context: Context, initColor: Int, onSelect: (color: Int) -> Unit) {
        val dp = context.resources.displayMetrics.density
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = (18 * dp).toInt()
            setPadding(p, p, p, p)
        }
        val lightness = SeekBar(context).apply {
            progressDrawable = generateSeekbarTrackDrawable(context)
            thumb = generateSeekbarThumbDrawable(context)
            splitTrack = false
            max = 300
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(initColor, hsl)
            progress = (hsl[2] * 300).toInt()
        }
        val saturation = SeekBar(context).apply {
            progressDrawable = generateSeekbarTrackDrawable(context)
            thumb = generateSeekbarThumbDrawable(context)
            splitTrack = false
            max = 300
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(initColor, hsl)
            progress = (hsl[1] * 300).toInt()
        }
        val hue = SeekBar(context).apply {
            progressDrawable = generateSeekbarTrackDrawable(context)
            thumb = generateSeekbarThumbDrawable(context)
            splitTrack = false
            max = 360
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(initColor, hsl)
            progress = hsl[0].toInt()
        }
        val colorText = EditText(context).apply {
            hint = formatColorString(ColorThemer.DEFAULT_BG)
            setTextColor(ColorThemer.contrast(0, 0.9, 0))
            setHintTextColor(ColorThemer.COLOR_HINT)
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
            val r = 8 * dp
            background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
            doOnTextChanged { text, _, _, _ ->
                val c = parseColorString(text.toString())
                backgroundTintList = ColorStateList.valueOf(c)
                setTextColor(ColorThemer.contrast(c, 0.9, c))
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                lightness.progress = (hsl[2] * 300).toInt()
                saturation.progress = (hsl[1] * 300).toInt()
                hue.progress = hsl[0].toInt()
            }
            setText(formatColorString(initColor))
        }
        lightness.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = parseColorString(colorText.text.toString())
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[2] = progress / 300f
                colorText.setText(formatColorString(ColorUtils.HSLToColor(hsl)))
            }
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
        })
        saturation.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = parseColorString(colorText.text.toString())
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[1] = progress / 300f
                colorText.setText(formatColorString(ColorUtils.HSLToColor(hsl)))
            }
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
        })
        hue.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val c = parseColorString(colorText.text.toString())
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(c, hsl)
                hsl[0] = progress.toFloat()
                colorText.setText(formatColorString(ColorUtils.HSLToColor(hsl)))
            }
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
        })

        val w = Dialog(context).apply {
            setContentView(content, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            val r = 24 * dp
            window!!.setBackgroundDrawable(ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)).apply {
                paint.color = ColorThemer.COLOR_CARD
            })
        }

        content.addView(GridLayout(context).apply {
            orientation = GridLayout.VERTICAL
            addColorView(colorText, 0x000000)
            addColorView(colorText, ColorThemer.DEFAULT_BG)
            addColorView(colorText, 0xefefef)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addColorView(colorText, context.getColor(android.R.color.system_accent1_500))
                addColorView(colorText, context.getColor(android.R.color.system_accent2_500))
                addColorView(colorText, context.getColor(android.R.color.system_accent3_500))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val wallColors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (wallColors != null) {
                    addColorView(colorText, wallColors.primaryColor.toArgb())
                    val s = wallColors.secondaryColor
                    val t = wallColors.tertiaryColor
                    if (s != null)
                        addColorView(colorText, s.toArgb())
                    if (t != null)
                        addColorView(colorText, t.toArgb())
                }
            }
        }, MarginLayoutParams((320 * dp).toInt(), (128 * dp).toInt()).apply {
            bottomMargin = (8 * dp).toInt()
        })
        content.addView(colorText)
        val l = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                setTextColor(ColorThemer.COLOR_TEXT)
                text = "L"
            })
            addView(lightness, l)
        }, l)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                setTextColor(ColorThemer.COLOR_TEXT)
                text = "S"
            })
            addView(saturation, l)
        }, l)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                setTextColor(ColorThemer.COLOR_TEXT)
                text = "H"
            })
            addView(hue, l)
        }, l)
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply {
                setText(android.R.string.ok)
                setTextColor(ColorThemer.COLOR_TEXT)
                val p = (8 * dp).toInt()
                setPadding(p, p, p, p)
                typeface = Typeface.DEFAULT_BOLD
                setOnClickListener {
                    onSelect(parseColorString(colorText.text.toString()) or 0xff000000.toInt())
                    w.dismiss()
                }
            })
            addView(TextView(context).apply {
                setText(android.R.string.cancel)
                setTextColor(ColorThemer.COLOR_TEXT)
                val p = (8 * dp).toInt()
                setPadding(p, p, p, p)
                typeface = Typeface.DEFAULT_BOLD
                setOnClickListener { w.dismiss() }
            })
        })
        w.show()
    }

    fun formatColorString(@ColorInt color: Int): String =
        "#${(color and 0xffffff).toString(16).padStart(6, '0')}"

    private fun parseColorString(color: String): Int =
        try { color.toColorInt() } catch (_: Exception) { 0 }

    private fun ViewGroup.addColorView(target: EditText, color: Int) {
        if (tag == null)
            tag = 0
        val dp = resources.displayMetrics.density
        addView(View(context).apply {
            val r = 8 * dp
            background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
            backgroundTintList = ColorStateList.valueOf(color or 0xff000000.toInt())
            setOnClickListener {
                target.setText(formatColorString(color))
            }
        }, GridLayout.LayoutParams().apply {
            val m = (8 * dp).toInt()
            width = 0
            height = 0
            val r = tag as Int / 3
            val c = tag as Int % 3
            rowSpec = GridLayout.spec(r, 1, 1f)
            columnSpec = GridLayout.spec(c, 1, 1f)
            if (c != 0) leftMargin = m
            if (r != 0) topMargin = m
            tag = tag as Int + 1
        })
    }
}