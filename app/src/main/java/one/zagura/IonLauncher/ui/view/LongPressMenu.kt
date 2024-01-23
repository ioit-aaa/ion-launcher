package one.zagura.IonLauncher.ui.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
                addOption(R.string.app_info) {
                    w.dismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            .setData(Uri.parse("package:${item.packageName}")),
                        LauncherItem.createOpeningAnimation(it),
                    )
                }
                if (HiddenApps.isHidden(context.ionApplication.settings, item)) addOption(R.string.unhide) {
                    w.dismiss()
                    HiddenApps.show(it.context, item)
                }
                else addOption(R.string.hide) {
                    w.dismiss()
                    HiddenApps.hide(it.context, item)
                }
            }
        }
        w.showAtLocation(parent, gravity, xoff - (4 * dp).toInt(), yoff)
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

    private fun ViewGroup.addOption(@StringRes label: Int, onClick: (View) -> Unit) {
        val dp = resources.displayMetrics.density
        addView(TextView(context).apply {
            background = GradientDrawable().apply {
                color = ColorStateList.valueOf(ColorThemer.foreground(context))
                setStroke((2 * dp).toInt(), ColorThemer.background(context))
                cornerRadius = 99 * dp
            }
            setText(label)
            setTextColor(ColorThemer.background(context))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            val h = (20 * dp).toInt()
            val v = (12 * dp).toInt()
            setPadding(h, v, h, v)
            setOnClickListener(onClick)
        }, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (2 * dp).toInt()
        })
    }
}