package one.zagura.IonLauncher.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.View.DragShadowBuilder
import one.zagura.IonLauncher.ui.ionApplication

class ItemDragShadow(context: Context, val icon: Drawable) : DragShadowBuilder() {

    private val iconSize: Int
    init {
        val dp = context.resources.displayMetrics.density
        iconSize = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
    }

    override fun onDrawShadow(canvas: Canvas) {
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        canvas.save()
        val s = canvas.width.toFloat() / icon.intrinsicWidth
        canvas.scale(s, s)
        icon.draw(canvas)
        canvas.restore()
    }

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        outShadowSize.x = iconSize
        outShadowSize.y = iconSize
        val c = iconSize / 2
        outShadowTouchPoint.x = c
        outShadowTouchPoint.y = c
    }
}