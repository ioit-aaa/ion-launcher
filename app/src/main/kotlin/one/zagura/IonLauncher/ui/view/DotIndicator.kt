package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.view.updateLayoutParams

class DotIndicator(context: Context) : LinearLayout(context) {
    private var current = 0
    private var dotCount = 0

    var color: Int = 0
        set(value) {
            field = value
            updateDots()
        }

    fun setDotCount(dots: Int) {
        if (this.dotCount == dots)
            return
        if (dots <= 1) {
            isVisible = false
            return
        }
        isVisible = true
        this.dotCount = dots
        updateDots()
    }

    fun setPageOffset(position: Int, positionOffset: Float) {
        val zeroOffset = positionOffset == 0f
        if (zeroOffset && position == current) return
        if (position !in 0 until childCount) return

        val currentDot = getChildAt(position)
        val nextDot = getChildAt(position + 1) ?: return
        currentDot?.alpha = (1f - positionOffset) * 0.5f + 0.5f
        nextDot.alpha = 0.5f * positionOffset + 0.5f
        if (zeroOffset)
            current = position
        else if (positionOffset >= 0.99f)
            current = position + 1
    }

    private fun updateDots() {
        val childCount = childCount
        for (i in (childCount - 1) downTo dotCount)
            removeViewAt(i)
        val dp = resources.displayMetrics.density
        val r = 99 * dp
        val bg = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
            .apply { setTint(color) }
        val dotMargin = (4 * dp).toInt()
        val s = (6 * dp).toInt()
        current = current.coerceAtMost(dotCount - 1).coerceAtLeast(0)
        for (i in 0 until dotCount) {
            val reuse = i < size
            val dot = if (reuse) getChildAt(i).apply {
                updateLayoutParams<LayoutParams> {
                    marginEnd = if (i == dotCount - 1) 0 else dotMargin
                }
            } else View(context).also {
                it.background = bg
                addView(it, LayoutParams(s, s).apply {
                    marginStart = if (i == 0) 0 else dotMargin
                    marginEnd = if (i == dotCount - 1) 0 else dotMargin
                })
            }
            dot.alpha = if (i == current) 1f else 0.5f
        }
    }
}