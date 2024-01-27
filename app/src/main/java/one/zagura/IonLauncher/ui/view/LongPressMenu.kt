package one.zagura.IonLauncher.ui.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.ui.ionApplication

object LongPressMenu {

    private var current: PopupWindow? = null

    fun popup(parent: View, item: LauncherItem, gravity: Int, xoff: Int, yoff: Int) {
        val dp = parent.resources.displayMetrics.density
        dismissCurrent()
        val content = LinearLayout(parent.context)
        val w = PopupWindow(content, (128 * dp).toInt(), LayoutParams.WRAP_CONTENT, false)
        w.setOnDismissListener { current = null }
        with(content) {
            orientation = LinearLayout.VERTICAL
            if (item is App) {
                addOption(R.string.app_info, place = Place.First) {
                    w.dismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            .setData(Uri.parse("package:${item.packageName}")),
                        LauncherItem.createOpeningAnimation(it),
                    )
                }
                if (HiddenApps.isHidden(context.ionApplication.settings, item)) addOption(R.string.unhide, place = Place.Last) {
                    w.dismiss()
                    HiddenApps.show(it.context, item)
                }
                else addOption(R.string.hide, place = Place.Last) {
                    w.dismiss()
                    HiddenApps.hide(it.context, item)
                }
            }
        }
        w.showAtLocation(parent, gravity, xoff - (2 * dp).toInt(), yoff)
        current = w
    }

    fun dismissCurrent() {
        current?.dismiss()
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

    private fun ViewGroup.addOption(@StringRes label: Int, place: Place = Place.Other, onClick: (View) -> Unit) {
        val dp = resources.displayMetrics.density
        addView(TextView(context).apply {
            background = RippleDrawable(
                ColorStateList.valueOf(ColorThemer.background(context) and 0xffffff or 0x55000000),
                GradientDrawable().apply {
                    color = ColorStateList.valueOf(ColorThemer.foreground(context))
                    setStroke((2 * dp).toInt(), ColorThemer.background(context))
                    val bigR = 24 * dp
                    val smallR = 8 * dp
                    cornerRadii = when (place) {
                        Place.First -> floatArrayOf(bigR, bigR, bigR, bigR, smallR, smallR, smallR, smallR)
                        Place.Other -> floatArrayOf(smallR, smallR, smallR, smallR, smallR, smallR, smallR, smallR)
                        Place.Last -> floatArrayOf(smallR, smallR, smallR, smallR, bigR, bigR, bigR, bigR)
                    }
                }, null)
            setText(label)
            setTextColor(ColorThemer.background(context))
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