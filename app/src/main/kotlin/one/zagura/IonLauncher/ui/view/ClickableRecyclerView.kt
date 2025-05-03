package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView


class ClickableRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return y * y + x * x
    }

    private var lastX = 0f
    private var lastY = 0f
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_UP ->
                if (distSq(lastX, lastY, e.x, e.y) < 4 && findChildViewUnder(e.x, e.y) == null)
                    performClick()
        }
        return super.dispatchTouchEvent(e)
    }
}