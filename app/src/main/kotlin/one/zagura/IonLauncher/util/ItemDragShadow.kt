package one.zagura.IonLauncher.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.view.View.DragShadowBuilder
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.ui.ionApplication
import androidx.core.graphics.withSave

class ItemDragShadow(context: Context, item: LauncherItem) : DragShadowBuilder() {

    private var icon = IconLoader.loadIcon(context, item)
    private val iconSize: Int
    init {
        val dp = context.resources.displayMetrics.density
        iconSize = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
    }

    override fun onDrawShadow(canvas: Canvas) {
        val icon = icon
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        canvas.withSave {
            val w = width.toFloat()
            val iw = w * 2 / 3
            val o = (w - iw) / 2
            val s = iw / icon.intrinsicWidth
            translate(o, o)
            scale(s, s)
            icon.draw(this)
        }
    }

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        val s = iconSize * 3 / 2
        val c = s / 2
        outShadowSize.x = s
        outShadowSize.y = s
        outShadowTouchPoint.x = c
        outShadowTouchPoint.y = c
    }
}