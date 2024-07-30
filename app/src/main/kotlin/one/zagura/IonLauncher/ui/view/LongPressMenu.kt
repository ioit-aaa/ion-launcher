package one.zagura.IonLauncher.ui.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.settings.SettingsActivity
import one.zagura.IonLauncher.ui.ionApplication

object LongPressMenu {

    private var current: PopupWindow? = null

    fun popup(parent: View, item: LauncherItem, gravity: Int, xoff: Int, yoff: Int, inDrawer: Boolean) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = PopupWindow(content, (128 * dp).toInt(), LayoutParams.WRAP_CONTENT, false)
        w.setOnDismissListener { current = null }
        with(content) {
            orientation = LinearLayout.VERTICAL
            if (item is App) {
                val bg = if (inDrawer) ColorThemer.drawerForeground(context)
                    else ColorThemer.cardBackgroundOpaque(context)
                val fg = if (inDrawer) ColorThemer.drawerBackgroundOpaque(context)
                    else ColorThemer.cardForeground(context)
                addOption(R.string.app_info, bg, fg, place = Place.First) {
                    w.dismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            .setData(Uri.parse("package:${item.packageName}"))
                    )
                }
                if (HiddenApps.isHidden(context.ionApplication.settings, item)) addOption(R.string.unhide, bg, fg, place = Place.Last) {
                    w.dismiss()
                    HiddenApps.show(it.context, item)
                }
                else addOption(R.string.hide, bg, fg, place = Place.Last) {
                    w.dismiss()
                    HiddenApps.hide(it.context, item)
                }
            }
        }
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt(), yoff)
        current = w
    }

    fun popupLauncher(parent: View, gravity: Int, xoff: Int, yoff: Int) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = PopupWindow(content, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true)
        w.setOnDismissListener { current = null }
        with(content) {
            val bg = ColorThemer.cardBackgroundOpaque(context)
            val fg = ColorThemer.cardForeground(context)
            orientation = LinearLayout.VERTICAL
            addOption(R.string.tweaks, bg, fg, place = Place.First) {
                w.dismiss()
                context.startActivity(Intent(context, SettingsActivity::class.java), LauncherItem.createOpeningAnimation(it))
                SuggestionsManager.onItemOpened(context, App(BuildConfig.APPLICATION_ID, SettingsActivity::class.java.name, Process.myUserHandle()))
            }
            addOption(R.string.wallpaper, bg, fg, place = Place.Last) {
                w.dismiss()
                context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER), LauncherItem.createOpeningAnimation(it))
            }
        }
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt(), yoff)
        current = w
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
                    setStroke((1.5f * dp).toInt(), fg)
                    val bigR = 21 * dp
                    val smallR = 8 * dp
                    cornerRadii = when (place) {
                        Place.First -> floatArrayOf(bigR, bigR, bigR, bigR, smallR, smallR, smallR, smallR)
                        Place.Other -> floatArrayOf(smallR, smallR, smallR, smallR, smallR, smallR, smallR, smallR)
                        Place.Last -> floatArrayOf(smallR, smallR, smallR, smallR, bigR, bigR, bigR, bigR)
                    }
                }, null)
            setText(label)
            setTextColor(fg)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            val h = (20 * dp).toInt()
            val v = (14 * dp).toInt()
            setPadding(h, v, h, v)
            setOnClickListener(onClick)
        }, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            if (place != Place.First)
                topMargin = (2 * dp).toInt()
        })
    }
}