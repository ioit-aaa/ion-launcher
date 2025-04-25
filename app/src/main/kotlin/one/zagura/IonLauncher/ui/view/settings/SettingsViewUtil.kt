@file:OptIn(ExperimentalContracts::class)

package one.zagura.IonLauncher.ui.view.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.StateSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.Utils
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SettingsPageScope(val view: ViewGroup)

class SettingViewScope(
    val updateSubtitle: (String) -> Unit,
    val view: ViewGroup,
)

fun Activity.setupWindow() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        window.setDecorFitsSystemWindows(false)
    else
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    window.setBackgroundDrawable(FillDrawable(resources.getColor(R.color.color_bg)))
    val bc = resources.getColor(R.color.color_bg) and 0xffffff or 0x55000000
    window.statusBarColor = bc
    window.navigationBarColor = bc
}

@OptIn(ExperimentalContracts::class)
inline fun Activity.setSettingsContentView(@StringRes titleId: Int, builder: SettingsPageScope.() -> Unit) {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val dp = resources.displayMetrics.density
    setupWindow()
    setContentView(NestedScrollView(this).apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, Utils.getNavigationBarHeight(context))
            addView(TextView(context).apply {
                textSize = 42f
                gravity = Gravity.START or Gravity.BOTTOM
                val h = (20 * dp).toInt()
                setPadding(h, Utils.getStatusBarHeight(context).coerceAtLeast((64 * dp).toInt()), h, h)
                setTextColor(resources.getColor(R.color.color_hint))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    typeface = Typeface.create(null, 500, false)
                setText(titleId)
            }, LayoutParams(MATCH_PARENT, (256 * dp).toInt() + Utils.getStatusBarHeight(context)))
            builder(SettingsPageScope(this))
        }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    })
    Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)
}

fun SettingsPageScope.title(@StringRes text: Int) {
    val dp = view.resources.displayMetrics.density
    view.addView(View(view.context).apply {
        background = FillDrawable(resources.getColor(R.color.color_separator))
    }, MarginLayoutParams(MATCH_PARENT, dp.toInt()).apply {
        topMargin = (14 * dp).toInt()
    })
    view.addView(TextView(view.context).apply {
        gravity = Gravity.CENTER_VERTICAL
        val h = (20 * dp).toInt()
        setPadding(h, (20 * dp).toInt(), h, (10 * dp).toInt())
        setText(text)
        setTextColor(resources.getColor(R.color.color_heading))
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        isAllCaps = true
    }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
}

inline fun SettingsPageScope.setting(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    isVertical: Boolean = false,
    child: SettingViewScope.() -> Unit
) = setting(title, view.context.getString(subtitle), isVertical, child)

@OptIn(ExperimentalContracts::class)
inline fun SettingsPageScope.setting(
    @StringRes title: Int,
    subtitle: String? = null,
    isVertical: Boolean = false,
    child: SettingViewScope.() -> Unit
): View {
    contract { callsInPlace(child, InvocationKind.EXACTLY_ONCE) }
    val ll = LinearLayout(view.context).apply {
        orientation = if (isVertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val dp = resources.displayMetrics.density
        minimumHeight = (48 * dp).toInt()
        val h = (20 * dp).toInt()
        val v = (12 * dp).toInt()
        setPadding(h, v, h, v)
        var updateSubtitle: (String) -> Unit = {}
        if (isVertical) {
            addView(TextView(context).apply {
                setText(title)
                setTextColor(resources.getColor(R.color.color_text))
                textSize = 20f
            }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (subtitle != null) {
                val sub = TextView(context).apply {
                    text = subtitle
                    setTextColor(resources.getColor(R.color.color_hint))
                    textSize = 15f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        typeface = Typeface.create(null, 300, false)
                }
                addView(sub, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                updateSubtitle = sub::setText
            }
        } else {
            if (subtitle == null) {
                addView(TextView(context).apply {
                    setText(title)
                    setTextColor(resources.getColor(R.color.color_text))
                    textSize = 20f
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            } else {
                val sub = TextView(context).apply {
                    text = subtitle
                    setTextColor(resources.getColor(R.color.color_hint))
                    textSize = 15f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        typeface = Typeface.create(null, 300, false)
                }
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(context).apply {
                        setText(title)
                        setTextColor(resources.getColor(R.color.color_text))
                        textSize = 20f
                    }, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    addView(sub, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                updateSubtitle = sub::setText
            }
        }
        child(SettingViewScope(updateSubtitle, this))
    }
    view.addView(ll, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    return ll
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
fun SettingViewScope.switch(settingId: String, default: Boolean, listener: (View, Boolean) -> Unit = { _, _ -> }) {
    val switch = CheckBox(view.context).apply {
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), StateSet.WILD_CARD),
            intArrayOf(context.resources.getColor(R.color.color_text), context.resources.getColor(R.color.color_disabled))
        )
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
    view.background = RippleDrawable(
        ColorStateList.valueOf(view.resources.getColor(R.color.color_disabled)),
        ColorDrawable(view.resources.getColor(R.color.color_bg)), null)
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
fun SettingViewScope.permissionSwitch(default: Boolean, listener: (View) -> Unit): (Boolean) -> Unit {
    val switch = CheckBox(view.context).apply {
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), StateSet.WILD_CARD),
            intArrayOf(context.resources.getColor(R.color.color_text), context.resources.getColor(R.color.color_disabled))
        )
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
    view.background = RippleDrawable(
        ColorStateList.valueOf(view.resources.getColor(R.color.color_disabled)),
        ColorDrawable(view.resources.getColor(R.color.color_bg)), null)
    return {
        switch.isChecked = it
        switch.isEnabled = !it
    }
}

fun SettingViewScope.onClick(activity: String) = onClick(Class.forName(activity))

fun SettingViewScope.onClick(activity: Class<*>) = onClick {
    it.context.startActivity(Intent(it.context, activity))
}

fun SettingViewScope.onClick(listener: (View) -> Unit) {
    view.setOnClickListener(listener)
    val dp = view.context.resources.displayMetrics.density
    val s = (32 * dp).toInt()
    val p = (4 * dp).toInt()
    view.addView(ImageView(view.context).apply {
        setImageResource(R.drawable.arrow_right)
        imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_hint))
        setPadding(p, p, p, p)
    }, LayoutParams(s, s))
    view.background = RippleDrawable(
        ColorStateList.valueOf(view.resources.getColor(R.color.color_disabled)),
        ColorDrawable(view.resources.getColor(R.color.color_bg)), null)
}

fun SettingViewScope.color(settingId: String, default: ColorThemer.ColorSetting) {
    val dp = view.context.resources.displayMetrics.density
    val s = (32 * dp).toInt()
    var color = view.context.ionApplication.settings[settingId, default].get(view.context) or 0xff000000.toInt()
    val dr = GradientDrawable().apply {
        setColor(color)
        setStroke(dp.coerceAtMost(2f).toInt(), 0x33000000)
        cornerRadius = s.toFloat()
    }

    view.setOnClickListener {
        val settings = it.context.ionApplication.settings
        ColorPicker.show(
            it.context,
            settings[settingId, default],
        ) { newColor ->
            val c = newColor.get(it.context)
            color = c or 0xff000000.toInt()
            dr.setColor(color)
            settings.edit(it.context) {
                settingId set newColor
            }
            updateSubtitle(ColorPicker.formatColorString(c))
        }
    }
    updateSubtitle(ColorPicker.formatColorString(color))
    view.addView(View(view.context).apply {
        background = dr
    }, LayoutParams(s, s))
    view.background = RippleDrawable(
        ColorStateList.valueOf(view.resources.getColor(R.color.color_disabled)),
        ColorDrawable(view.resources.getColor(R.color.color_bg)), null)
}

fun SettingViewScope.seekbar(
    settingId: String,
    default: Int,
    min: Int,
    max: Int,
    multiplier: Int = 1,
) {
    val dp = view.resources.displayMetrics.density
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
        minWidth = (48 * dp).toInt()
        gravity = Gravity.CENTER
        val r = 99 * dp
        background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)).apply {
            paint.color = resources.getColor(R.color.color_bg_sunk) and 0xffffff or 0x66000000
        }
        val h = (12 * dp).toInt()
        val v = (4 * dp).toInt()
        setPadding(h, v, h, v)
        textSize = 16f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            typeface = Typeface.create(null, 300, false)
        includeFontPadding = false
        setTextColor(resources.getColor(R.color.color_button_text))
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
        val v = (6 * dp).toInt()
        setPadding(0, v, 0, 0)
        addView(number, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        addView(seekBar, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    })
}

fun generateSeekbarTrackDrawable(context: Context): Drawable {

    fun generateBG(color: Int) = GradientDrawable().apply {
        cornerRadius = Float.MAX_VALUE
        setColor(color)
    }

    val dp = context.resources.displayMetrics.density
    val a = (2 * dp).toInt()

    @SuppressLint("RtlHardcoded")
    // For some reason it flips the drawable in RTL mode, so .LEFT gives the desired behavior
    val out = LayerDrawable(arrayOf(
        generateBG(context.resources.getColor(R.color.color_bg_sunk)).apply {
            setSize(0, a)
        },
        ClipDrawable(generateBG(context.resources.getColor(R.color.color_hint)).apply {
            setSize(0, a)
        }, Gravity.LEFT, ClipDrawable.HORIZONTAL)
    ))
    val h = (0 * dp).toInt()
    val inset = (12 * dp).toInt()
    out.setLayerInset(0, h, inset, h, inset)
    out.setLayerInset(1, h, inset, h, inset)
    out.setId(0, android.R.id.background)
    out.setId(1, android.R.id.progress)
    return out
}

fun generateSeekbarThumbDrawable(context: Context): Drawable {
    val dp = context.resources.displayMetrics.density
    val r = (18 * dp).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(context.resources.getColor(R.color.color_text))
        setSize(r, r)
    }
}