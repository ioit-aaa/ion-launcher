package one.zagura.IonLauncher.ui.view

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.ContactItem
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

object LongPressMenu {

    private var current: PopupWindow? = null

    fun popup(parent: View, item: LauncherItem, gravity: Int, xoff: Int, yoff: Int, inDrawer: Boolean) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = PopupWindow(content, (192 * dp).toInt(), LayoutParams.WRAP_CONTENT, false)
        w.setOnDismissListener { current = null }
        val p = (12 * dp).toInt()
        with(content) {
            setPadding(p, p, p, p)
            clipToPadding = false
            orientation = LinearLayout.VERTICAL
            val bg = if (inDrawer) ColorThemer.drawerForeground(context)
                else ColorThemer.cardBackgroundOpaque(context)
            val fg = if (inDrawer) ColorThemer.drawerBackgroundOpaque(context)
                else ColorThemer.cardForeground(context)
            if (item is App) {
                addOption(R.string.app_info, bg, fg, place = Place.First) {
                    w.dismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            .setData(Uri.parse("package:${item.packageName}"))
                    )
                }
                if (HiddenApps.isHidden(context.ionApplication.settings, item)) addOption(R.string.unhide, bg, fg, place = Place.Other) {
                    w.dismiss()
                    HiddenApps.show(it.context, item)
                }
                else addOption(R.string.hide, bg, fg, place = Place.Other) {
                    w.dismiss()
                    HiddenApps.hide(it.context, item)
                }
            }
            if (item is App || item is StaticShortcut) {
                addOption(R.string.edit, bg, fg, place = if (item is App) Place.Last else Place.Other) {
                    w.dismiss()
                    popupEdit(parent, item)
                }
            }
        }
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt() - p, yoff - p)
        current = w
    }

    fun popupLauncher(parent: View, gravity: Int, xoff: Int, yoff: Int) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = PopupWindow(content, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true)
        w.setOnDismissListener { current = null }
        val p = (12 * dp).toInt()
        with(content) {
            setPadding(p, p, p, p)
            clipToPadding = false
            val bg = ColorThemer.cardBackgroundOpaque(context)
            val fg = ColorThemer.cardForeground(context)
            orientation = LinearLayout.VERTICAL
            addOption(R.string.tweaks, bg, fg, place = Place.First) {
                w.dismiss()
                context.startActivity(Intent(context, SettingsActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK), LauncherItem.createOpeningAnimation(it))
            }
            addOption(R.string.wallpaper, bg, fg, place = Place.Last) {
                w.dismiss()
                context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER), LauncherItem.createOpeningAnimation(it))
            }
        }
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt() - p, yoff)
        current = w
    }

    fun popupEdit(parent: View, item: LauncherItem) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = Dialog(parent.context).apply {
            setContentView(content)
            val r = 26 * dp
            window!!.setBackgroundDrawable(ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)).apply {
                paint.color = context.resources.getColor(R.color.color_bg)
            })
        }
        with(content) {
            val p = (12 * dp).toInt()
            setPadding(p, p, p, p)
            orientation = LinearLayout.VERTICAL

            val settings = parent.context.ionApplication.settings
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()

            val icon = ImageView(parent.context).apply {
                setImageDrawable(IconLoader.loadIcon(context, item))
                setOnClickListener {
                    w.dismiss()
                    CustomIconActivity.start(parent.context, item)
                }
            }
            val label = EditText(parent.context).apply {
                setText(LabelLoader.loadLabel(context, item))
                setTextColor(resources.getColor(R.color.color_text))
            }
            addView(icon, LayoutParams(iconSize, iconSize))
            addView(label)
            addView(TextView(context).apply {
                val br = 10 * dp
                val h = (32 * dp).toInt()
                val v = (15 * dp).toInt()
                setText(android.R.string.ok)
                setTextColor(resources.getColor(R.color.color_button_text))
                background = RippleDrawable(
                    ColorStateList.valueOf(resources.getColor(R.color.color_hint)),
                    ShapeDrawable(RoundRectShape(floatArrayOf(br, br, br, br, br, br, br, br), null, null)).apply {
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
            background = RippleDrawable(
                ColorStateList.valueOf(fg and 0xffffff or 0x55000000),
                GradientDrawable().apply {
                    color = ColorStateList.valueOf(bg)
                    val bigR = 21 * dp
                    val smallR = 8 * dp
                    cornerRadii = when (place) {
                        Place.First -> floatArrayOf(bigR, bigR, bigR, bigR, smallR, smallR, smallR, smallR)
                        Place.Other -> floatArrayOf(smallR, smallR, smallR, smallR, smallR, smallR, smallR, smallR)
                        Place.Last -> floatArrayOf(smallR, smallR, smallR, smallR, bigR, bigR, bigR, bigR)
                    }
                }, null)
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