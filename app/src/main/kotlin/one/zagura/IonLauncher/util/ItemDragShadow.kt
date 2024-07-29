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
        val w = canvas.width.toFloat()
        val iw = w * 2 / 3
        val o = (w - iw) / 2
        val s = iw / icon.intrinsicWidth
        canvas.scale(s, s)
        canvas.translate(o, o)
        icon.draw(canvas)
        canvas.restore()
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