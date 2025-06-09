package one.zagura.IonLauncher.ui.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.Popup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.EditedItems
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.ui.settings.SettingsActivity
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.settings.customIconPicker.CustomIconActivity
import one.zagura.IonLauncher.util.Utils
import one.zagura.IonLauncher.util.drawable.SquircleRectShape
import one.zagura.IonLauncher.util.drawable.UniformSquircleRectShape
import androidx.core.net.toUri

object LongPressMenu {

    private var current: PopupWindow? = null

    enum class Where {
        DRAWER, SUGGESTION, DOCK
    }

    fun popupIcon(parent: View, item: LauncherItem, iconX: Int, iconY: Int, iconSize: Int, where: Where) {
        val sh = parent.resources.displayMetrics.heightPixels
        val sw = parent.resources.displayMetrics.widthPixels
        val dp = parent.resources.displayMetrics.density
        val v = (8 * dp).toInt()
        val (gravityY, y) = if (iconY > sh / 2)
            Gravity.BOTTOM to sh +
                    Utils.getStatusBarHeight(parent.context) +
                    Utils.getNavigationBarHeight(parent.context) - iconY
        else Gravity.TOP to iconY + iconSize
        val (gravityX, x) = if (iconX > sw / 2)
            Gravity.END to sw - iconX - iconSize
        else Gravity.START to iconX
        popup(parent, item, gravityX or gravityY, x + (-2 * dp).toInt(), y + v, where)
    }

    fun popup(parent: View, item: LauncherItem, gravity: Int, xoff: Int, yoff: Int, where: Where) {
        val l = ArrayList<Pair<Int, (View) -> Unit>>()
        if (item is App) {
            l.add(R.string.app_info to {
                parent.context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .setData("package:${item.packageName}".toUri()))
            })
            if (where != Where.DOCK)
                l.add(if (HiddenApps.isHidden(parent.context.ionApplication.settings, item))
                    R.string.unhide to { HiddenApps.show(it.context, item) }
                else R.string.hide to { HiddenApps.hide(it.context, item) })
        }
        if (item is App || item is StaticShortcut)
            l.add(R.string.edit to { popupEdit(parent, item) })
        val w = customPopup(parent.context, *l.toTypedArray())
        val dp = parent.resources.displayMetrics.density
        val p = (12 * dp).toInt()
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt() - p, yoff - p)
        w.contentView.run {
            scaleX = 0f
            scaleY = 0f
            doOnLayout {
                pivotX = if (gravity and Gravity.START == Gravity.START) 0f
                else width.toFloat()
                pivotY = if (gravity and Gravity.TOP == Gravity.TOP) 0f
                else height.toFloat()
            }
            animate().scaleX(1f).scaleY(1f).duration = 60L
        }
    }

    fun popupSmartspacer(
        context: Context,
        anchorView: View,
        target: SmartspaceTarget,
        launchIntent: (Intent?) -> Unit,
        dismissAction: ((SmartspaceTarget) -> Unit)?,
        aboutIntent: Intent?,
        feedbackIntent: Intent?,
        settingsIntent: Intent?,
    ) : Popup {
        val w = customPopup(context, *listOfNotNull(
            if (settingsIntent != null)
                R.string.settings to { v: View -> launchIntent(settingsIntent) }
            else null,
            R.string.tweaks to { v: View ->
                context.startActivity(Intent(context, SettingsActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK), LauncherItem.createOpeningAnimation(v).toBundle())
            },
            R.string.wallpaper to { v: View ->
                context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER), LauncherItem.createOpeningAnimation(v).toBundle())
            },
        ).toTypedArray())
        w.showAtLocation(anchorView, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, anchorView.height)
        return object : Popup {
            override fun dismiss() = w.dismiss()
        }
    }

    fun popupLauncher(parent: View, gravity: Int, xoff: Int, yoff: Int) {
        val w = customPopup(parent.context,
            R.string.tweaks to {
                it.context.startActivity(Intent(it.context, SettingsActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK), LauncherItem.createOpeningAnimation(it).toBundle())
            },
            R.string.wallpaper to {
                it.context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER), LauncherItem.createOpeningAnimation(it).toBundle())
            })
        w.showAtLocation(parent, gravity, xoff, yoff)
        w.contentView.run {
            scaleX = 0f
            scaleY = 0f
            doOnLayout {
                if (gravity and Gravity.END == Gravity.END) pivotX = width.toFloat()
                if (gravity and Gravity.BOTTOM == Gravity.BOTTOM) pivotY = height.toFloat()
            }
            animate().scaleX(1f).scaleY(1f).duration = 60L
        }
    }

    fun customPopup(context: Context, vararg items: Pair<Int, (View) -> Unit>): PopupWindow {
        val dp = context.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(context)
        val w = PopupWindow(content, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true)
        w.setOnDismissListener { current = null }
        val p = (12 * dp).toInt()
        with(content) {
            setPadding(p, p, p, p)
            clipToPadding = false
            val bg = ColorThemer.cardBackgroundOpaque(context)
            val fg = ColorThemer.cardForeground(context)
            orientation = LinearLayout.VERTICAL
            for ((i, item) in items.withIndex()) {
                addOption(item.first, bg, fg, when (i) {
                    0 -> Place.First
                    items.lastIndex -> Place.Last
                    else -> Place.Other
                }) {
                    w.dismiss()
                    item.second(it)
                }
            }
        }
        current = w
        return w
    }

    private fun popupEdit(parent: View, item: LauncherItem) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = Dialog(parent.context).apply {
            setContentView(content)
            val r = 26 * dp
            window!!.setBackgroundDrawable(ShapeDrawable(UniformSquircleRectShape(r)).apply {
                paint.color = context.resources.getColor(R.color.color_bg)
            })
        }
        with(content) {
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            val settings = context.ionApplication.settings
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()

            val icon = FrameLayout(context).apply {
                addView(ImageView(context).apply {
                    setImageDrawable(IconLoader.loadIcon(context, item))
                    setOnClickListener {
                        w.dismiss()
                        CustomIconActivity.start(parent.context, item)
                    }
                }, FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER))
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.edit)
                    imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_bg))
                    background = ShapeDrawable(OvalShape()).apply {
                        paint.color = resources.getColor(R.color.color_text)
                    }
                    val p = iconSize / 18
                    setPadding(p, p, p, p)
                }, FrameLayout.LayoutParams(iconSize / 2, iconSize / 2, Gravity.BOTTOM or Gravity.END))
            }
            val label = EditText(parent.context).apply {
                setText(LabelLoader.loadLabel(context, item))
                setTextColor(resources.getColor(R.color.color_text))
            }
            addView(icon, MarginLayoutParams(iconSize * 6 / 5, iconSize * 6 / 5).apply {
                val m = (24 * dp).toInt()
                leftMargin = m
                rightMargin = m
                topMargin = m
                bottomMargin = m
            })
            addView(label, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                val m = (16 * dp).toInt()
                leftMargin = m
                rightMargin = m
                bottomMargin = m
            })
            addView(TextView(context).apply {
                val br = 10 * dp
                val h = (32 * dp).toInt()
                val v = (15 * dp).toInt()
                setText(android.R.string.ok)
                setTextColor(resources.getColor(R.color.color_button_text))
                background = RippleDrawable(
                    ColorStateList.valueOf(resources.getColor(R.color.color_hint)),
                    ShapeDrawable(UniformSquircleRectShape(br)).apply {
                        paint.color = resources.getColor(R.color.color_button)
                    }, null)
                setPadding(h, v, h, v)
                gravity = Gravity.CENTER_HORIZONTAL
                typeface = Typeface.DEFAULT_BOLD
                setSingleLine()
                isAllCaps = true
                setOnClickListener { w.dismiss() }
            }, LayoutParams((192 * dp).toInt(), LayoutParams.WRAP_CONTENT))
            w.setOnDismissListener {
                current = null
                EditedItems.setLabel(context, settings, item, label.text.toString())
                if (item is App)
                    HiddenApps.reshow(context, item)
            }
        }
        w.show()
    }

    fun dismissCurrent(): Boolean {
        current?.dismiss()
            ?: return false
        return true
    }

    fun isInFocus(): Boolean {
        val c = current ?: return false
        return c.contentView.hasWindowFocus()
    }

    fun onDragEnded() {
        current?.run {
            isFocusable = true
            update()
        }
    }

    private enum class Place {
        First, Other, Last
    }

    private fun ViewGroup.addOption(@StringRes label: Int, bg: Int, fg: Int, place: Place = Place.Other, onClick: (View) -> Unit) {
        val dp = resources.displayMetrics.density
        addView(TextView(context).apply {
            val bigR = 21 * dp
            val smallR = 8 * dp
            background = RippleDrawable(
                ColorStateList.valueOf(fg and 0xffffff or 0x55000000),
                ShapeDrawable(SquircleRectShape(when (place) {
                    Place.First -> floatArrayOf(bigR, bigR, smallR, smallR)
                    Place.Other -> floatArrayOf(smallR, smallR, smallR, smallR)
                    Place.Last -> floatArrayOf(smallR, smallR, bigR, bigR)
                })).apply { paint.color = bg }, null)
            elevation = 4 * dp
            setText(label)
            setTextColor(fg)
            textSize = 16f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                typeface = Typeface.create(null, 500, false)
            val h = (20 * dp).toInt()
            val v = (14 * dp).toInt()
            setPadding(h, v, h, v)
            setOnClickListener(onClick)
        }, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            if (place != Place.First)
                topMargin = (4 * dp).toInt()
        })
    }
}