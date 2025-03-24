package one.zagura.IonLauncher.util

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import one.zagura.IonLauncher.ui.view.Gestures
import kotlin.math.abs

object SlideGestureHelper {
    private var shadeExpanded = false

    fun onTouchEvent(context: Context, e: MotionEvent) {
        if (shadeExpanded && (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL))
            Utils.setWindowSlippery((context as Activity).window, false)
        if (e.action != MotionEvent.ACTION_MOVE)
            shadeExpanded = false
    }

    fun onScroll(context: Context, e1: MotionEvent?, e2: MotionEvent): Boolean {
        if (e1 == null || shadeExpanded)
            return false
        val dy = e2.y - e1.y
        val dx = e2.x - e1.x
        if (abs(dy) < abs(dx)) {
            // todo
        } else if (dy > 0) {
            Utils.setWindowSlippery((context as Activity).window, true)
            if (Gestures.onFlingDown(context))
                shadeExpanded = true
        }
        return true
    }

    fun onFling(context: Context, vx: Float, vy: Float): Boolean {
        if (abs(vy) < abs(vx)) {
            return if (vx > 0)
                Gestures.onFlingLtR(context)
            else
                Gestures.onFlingRtL(context)
        } else {
            if (vy > 0)
                return Gestures.onFlingDown(context)
        }
        return false
    }
}