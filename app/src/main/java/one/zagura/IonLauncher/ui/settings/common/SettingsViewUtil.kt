@file:OptIn(ExperimentalContracts::class)

package one.zagura.IonLauncher.ui.settings.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.util.StateSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.Utils
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ViewBuilderScope(val view: ViewGroup)

fun Activity.setupWindow() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        window.setDecorFitsSystemWindows(false)
    else
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    window.setBackgroundDrawable(FillDrawable(ColorThemer.COLOR_CARD))
    val bc = ColorThemer.COLOR_CARD and 0xffffff or 0x55000000
    window.statusBarColor = bc
    window.navigationBarColor = bc
}

inline fun Activity.setSettingsContentView(@StringRes titleId: Int, builder: ViewBuilderScope.() -> Unit) {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val dp = resources.displayMetrics.density
    setupWindow()
    setContentView(NestedScrollView(this).apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, Utils.getNavigationBarHeight(context))
            addView(TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                val h = (20 * dp).toInt()
                setPadding(h, Utils.getStatusBarHeight(context), h, 0)
                textSize = 22f
                setTextColor(ColorThemer.COLOR_HINT)
                background = FillDrawable(ColorThemer.COLOR_CARD)
                setText(titleId)
            }, LayoutParams(MATCH_PARENT, (64 * dp).toInt() + Utils.getStatusBarHeight(context)))
            addView(View(context).apply {
                background = FillDrawable(ColorThemer.COLOR_SEPARATOR)
            }, LayoutParams(MATCH_PARENT, dp.toInt()))
            builder(ViewBuilderScope(this))
        }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    })
}

fun ViewBuilderScope.title(@StringRes text: Int) {
    val dp = view.resources.displayMetrics.density
    view.addView(TextView(view.context).apply {
        gravity = Gravity.CENTER_VERTICAL
        val h = (20 * dp).toInt()
        setPadding(h, (24 * dp).toInt(), h, (6 * dp).toInt())
        setText(text)
        setTextColor(ColorThemer.COLOR_HINT)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
    }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
}

inline fun ViewBuilderScope.setting(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    isVertical: Boolean = false,
    child: ViewBuilderScope.() -> Unit
) = setting(title, view.context.getString(subtitle), isVertical, child)

inline fun ViewBuilderScope.setting(
    @StringRes title: Int,
    subtitle: String? = null,
    isVertical: Boolean = false,
    child: ViewBuilderScope.() -> Unit
): View {
    contract { callsInPlace(child, InvocationKind.EXACTLY_ONCE) }
    val ll = LinearLayout(view.context).apply {
        orientation = if (isVertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val dp = resources.displayMetrics.density
        minimumHeight = (48 * dp).toInt()
        val h = (20 * dp).toInt()
        val v = (6 * dp).toInt()
        setPadding(h, v, h, v)
        if (isVertical) {
            addView(TextView(context).apply {
                setText(title)
                setTextColor(ColorThemer.COLOR_TEXT)
                textSize = 18f
            }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (subtitle != null) {
                addView(TextView(context).apply {
                    text = subtitle
                    setTextColor(ColorThemer.COLOR_TEXT)
                    textSize = 14f
                }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        } else {
            if (subtitle == null) {
                addView(TextView(context).apply {
                    setText(title)
                    setTextColor(ColorThemer.COLOR_TEXT)
                    textSize = 18f
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            } else {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(context).apply {
                        setText(title)
                        setTextColor(ColorThemer.COLOR_TEXT)
                        textSize = 18f
                    }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    addView(TextView(context).apply {
                        text = subtitle
                        setTextColor(ColorThemer.COLOR_TEXT)
                        textSize = 14f
                    }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }
        }
        child(ViewBuilderScope(this))
    }
    view.addView(ll, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    return ll
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
fun ViewBuilderScope.switch(settingId: String, default: Boolean, listener: (View, Boolean) -> Unit = { _, _ -> }) {
    val switch = Switch(view.context).apply {
        trackDrawable = generateSwitchTrackDrawable()
        thumbDrawable = generateSwitchThumbDrawable(context)
        isChecked = context.ionApplication.settings[settingId, default]
        setOnCheckedChangeListener { v, isChecked ->
            Utils.click(v.context)
            listener(v, isChecked)
            v.context.ionApplication.settings.edit(v.context) {
                settingId set isChecked
            }
        }
    }
    view.setOnClickListener {
        switch.isChecked = !switch.isChecked
    }
    view.addView(switch)
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
fun ViewBuilderScope.permissionSwitch(default: Boolean, listener: (View) -> Unit): (Boolean) -> Unit {
    val switch = Switch(view.context).apply {
        trackDrawable = generateSwitchTrackDrawable()
        thumbDrawable = generateSwitchThumbDrawable(context)
        isChecked = default
        isEnabled = !default
        setOnCheckedChangeListener { v, isChecked ->
            if (!isChecked)
                return@setOnCheckedChangeListener
            isEnabled = false
            Utils.click(v.context)
            listener(v)
        }
    }
    view.setOnClickListener {
        switch.isChecked = true
    }
    view.addView(switch)
    return {
        switch.isChecked = it
        switch.isEnabled = !it
    }
}

fun ViewBuilderScope.onClick(activity: Class<*>) = onClick {
    it.context.startActivity(Intent(it.context, activity))
}

fun ViewBuilderScope.onClick(listener: (View) -> Unit) {
    view.setOnClickListener(listener)
    val dp = view.context.resources.displayMetrics.density
    val s = (24 * dp).toInt()
    view.addView(ImageView(view.context).apply {
        setImageResource(R.drawable.ic_arrow_right)
        imageTintList = ColorStateList.valueOf(ColorThemer.COLOR_HINT)
    }, LayoutParams(s, s))
}

fun ViewBuilderScope.color(settingId: String, default: Int) {
    val dr = ShapeDrawable(OvalShape())
    dr.paint.color = view.context.ionApplication.settings[settingId, default] or 0xff000000.toInt()
    view.setOnClickListener {
        ColorPicker.show(
            it.context,
            it.context.ionApplication.settings[settingId, default]
        ) { newColor ->
            dr.paint.color = newColor or 0xff000000.toInt()
            dr.invalidateSelf()
            it.context.ionApplication.settings.edit(it.context) {
                settingId set newColor
            }
        }
    }
    val dp = view.context.resources.displayMetrics.density
    val s = (32 * dp).toInt()
    view.addView(View(view.context).apply {
        background = dr
    }, LayoutParams(s, s))
}

fun ViewBuilderScope.seekbar(
    settingId: String,
    default: Int,
    min: Int,
    max: Int,
    multiplier: Int = 1,
) {
    val number = EditText(view.context)
    val seekBar = SeekBar(view.context).apply {
        progressDrawable = generateSeekbarTrackDrawable(context)
        thumb = generateSeekbarThumbDrawable(context)
        splitTrack = false
        this.max = (max - min) / multiplier
        progress = (context.ionApplication.settings[settingId, default] - min) / multiplier
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                Utils.tick(v.context)
                val value = progress * multiplier + min
                v.context.ionApplication.settings.edit(v.context) {
                    settingId set value
                }
                number.setText(value.toString())
            }
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
        })
    }
    with(number) {
        textSize = 16f
        background = null
        setPadding(0)
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        setTextColor(ColorThemer.COLOR_HINT)
        setText(context.ionApplication.settings[settingId, default].toString())
        doOnTextChanged { text, _, _, _ ->
            val value = text.toString().toIntOrNull()
            if (value != null && value >= min && value <= max) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    seekBar.setProgress((value - min) / multiplier, true)
                else
                    seekBar.progress = (value - min) / multiplier
                context.ionApplication.settings.edit(context) {
                    settingId set value
                }
            }
        }
    }
    view.addView(LinearLayout(view.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(number, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        addView(seekBar, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    })
}

fun generateSeekbarTrackDrawable(context: Context): Drawable {
    val dp = context.resources.displayMetrics.density
    val out = LayerDrawable(arrayOf(
        generateBG(ColorThemer.COLOR_CARD_SUNK),
        ClipDrawable(generateBG(ColorThemer.COLOR_TEXT), Gravity.START, GradientDrawable.Orientation.BL_TR.ordinal)
    ))
    val h = (2 * dp).toInt()
    val inset = (7 * dp).toInt()
    out.setLayerInset(0, h, inset, h, inset)
    out.setLayerInset(1, h, inset, h, inset)
    out.setId(0, android.R.id.background)
    out.setId(1, android.R.id.progress)
    return out
}

fun generateSeekbarThumbDrawable(context: Context): Drawable {
    return generateCircle(context, ColorThemer.COLOR_CARD, ColorThemer.COLOR_TEXT)
}

private fun generateSwitchTrackDrawable(): Drawable {
    val out = StateListDrawable()
    out.addState(intArrayOf(android.R.attr.state_checked), generateBG(ColorThemer.COLOR_HINT))
    out.addState(StateSet.WILD_CARD, generateBG(0x0effffff))
    return out
}

private fun generateSwitchThumbDrawable(context: Context): Drawable {
    val out = StateListDrawable()
    out.addState(intArrayOf(android.R.attr.state_checked), generateCircle(context, ColorThemer.COLOR_CARD, ColorThemer.COLOR_TEXT))
    out.addState(StateSet.WILD_CARD, generateCircle(context, ColorThemer.COLOR_CARD, 0x2affffff))
    return out
}

private fun generateCircle(context: Context, color: Int, insideColor: Int): Drawable {
    val dp = context.resources.displayMetrics.density
    val r = (12 * dp).toInt()
    val inset = (5 * dp).toInt()
    return LayerDrawable(arrayOf(
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(r, r)
        },
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(insideColor)
            setSize(r, r)
        },
    )).apply {
        setLayerInset(0, inset, inset, inset, inset)
        val i = inset + (4 * dp).toInt()
        setLayerInset(1, i, i, i, i)
    }
}

private fun generateBG(color: Int): Drawable {
    return GradientDrawable().apply {
        cornerRadius = Float.MAX_VALUE
        setColor(color)
    }
}