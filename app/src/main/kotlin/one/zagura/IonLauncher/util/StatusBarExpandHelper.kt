package one.zagura.IonLauncher.util

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import kotlin.math.abs

object StatusBarExpandHelper {
    private var expanded = false

    fun onTouchEvent(context: Context, e: MotionEvent) {
        if (expanded && (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL))
            Utils.setWindowSlippery((context as Activity).window, false)
        if (e.action != MotionEvent.ACTION_MOVE)
            expanded = false
    }

    fun onScroll(context: Context, e1: MotionEvent?, e2: MotionEvent): Boolean {
        if (e1 == null || expanded)
            return false
        val dy = e2.y - e1.y
        val dx = e2.x - e1.x
        if (abs(dy) > abs(dx) && dy > 0) {
            expanded = true
            Utils.setWindowSlippery((context as Activity).window, true)
            Utils.pullStatusBar(context)
        }
        return true
    }
}