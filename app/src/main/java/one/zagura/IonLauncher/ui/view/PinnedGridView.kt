package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.DragEvent
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.view.updateLayoutParams
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.LiveWallpaper
import one.zagura.IonLauncher.util.NonDrawable
import one.zagura.IonLauncher.util.Utils
import kotlin.math.abs

@SuppressLint("UseCompatLoadingForDrawables")
class PinnedGridView(
    context: Context,
) : View(context) {

    class ItemPreview(val icon: Drawable, val x: Int, val y: Int)

    var showDropTargets = false
        set(value) {
            field = value
            invalidate()
        }
    private var replacePreview: ItemPreview? = null
    private var dropPreview: ItemPreview? = null
    private var highlight: Pair<Int, Int>? = null

    private var items = ArrayList<LauncherItem?>()
    private var columns = 0
    private var rows = 0
    private var iconSize = 0

    private fun getItem(x: Int, y: Int): LauncherItem? {
        val i = y * columns + x
        if (i >= items.size)
            return null
        return items[i]
    }

    private fun setItem(x: Int, y: Int, item: LauncherItem?) {
        val i = y * columns + x
        while (items.size <= i)
            items.add(null)
        items[i] = item
        Dock.setItem(context, i, item)
    }

    init {
        val dp = resources.displayMetrics.density
        val p = (8 * dp).toInt()
        setPadding(p, p, p, p)
    }

    fun applyCustomizations() {
        val dp = resources.displayMetrics.density
        updateLayoutParams {
            height = calculateGridHeight()
        }
        columns = context.ionApplication.settings["dock:columns", 5]
        rows = context.ionApplication.settings["dock:rows", 2]
        iconSize = context.ionApplication.settings["dock:icon-size", 48]

        val fg = ColorThemer.foreground(context)
        gridPaint.color = fg and 0xffffff or 0xdd000000.toInt()
        targetPaint.color = fg and 0xffffff or 0x88000000.toInt()

        updateGridApps()
    }

    /**
     * Called after [applyCustomizations] if necessary
     */
    fun calculateSideMargin(): Int {
        val dp = resources.displayMetrics.density
        val iconSize = (iconSize * dp).toInt()
        return (resources.displayMetrics.widthPixels / columns - iconSize) / 2 + paddingLeft
    }

    fun calculateGridHeight(): Int {
        val dp = resources.displayMetrics.density
        val iconSize = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
        val vMargin = (12 * dp).toInt()
        val rows = context.ionApplication.settings["dock:rows", 2]
        return paddingBottom + paddingTop + (iconSize + vMargin * 2) * rows
    }

    fun updateGridApps() {
        items = ArrayList(Dock.getItems(context))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        drawItems(canvas)
        if (!showDropTargets)
            return
        val dp = resources.displayMetrics.density
        val r = (iconSize / 2 + 1) * dp
        for (x in 0 ..< columns)
            for (y in 0 ..< rows) {
                canvas.drawCircle(
                    paddingLeft + (width - paddingLeft - paddingRight) / columns * (0.5f + x),
                    paddingTop + (height - paddingTop - paddingBottom) / rows * (0.5f + y),
                    r, targetPaint)
            }
        for (x in 0 .. columns)
            for (y in 0 .. rows) {
                canvas.drawCircle(
                    paddingLeft + (width - paddingLeft - paddingRight) / columns * x.toFloat(),
                    paddingTop + (height - paddingTop - paddingBottom) / rows * y.toFloat(),
                    dp, gridPaint)
            }
    }

    private fun drawItems(canvas: Canvas) {
        val dp = resources.displayMetrics.density
        val r = (iconSize * dp).toInt() / 2
        val sr = (iconSize * dp * 0.9f).toInt() / 2
        val br = (iconSize * dp).toInt() * 3 / 4
        val d = dropPreview
        val i = replacePreview
        for (x in 0 until columns)
            for (y in 0 until rows) {
                val r = if (highlight?.let { it.first == x && it.second == y } == true)
                    br else if (showDropTargets) sr else r
                val icon = if (d?.let { it.x == x && it.y == y } == true) d.icon
                    else if (i?.let { it.x == x && it.y == y } == true) i.icon
                    else IconLoader.loadIcon(context, getItem(x, y) ?: continue)
                val centerX = (paddingLeft + (width - paddingLeft - paddingRight) / columns * (0.5f + x)).toInt()
                val centerY = (paddingTop + (height - paddingTop - paddingBottom) / rows * (0.5f + y)).toInt()
                icon.setBounds(centerX - r, centerY - r, centerX + r, centerY + r)
                if (showDropTargets) {
                    val a = icon.alpha
                    icon.alpha = 200
                    icon.draw(canvas)
                    icon.alpha = a
                } else
                    icon.draw(canvas)
            }
    }

    private val gridPaint = Paint().apply {
        isAntiAlias = true
    }

    private val targetPaint = Paint().apply {
        val dp = resources.displayMetrics.density
        strokeWidth = 2 * dp
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private fun viewToGridCoords(vx: Int, vy: Int): Pair<Int, Int> {
        val gx = ((vx - paddingLeft) * columns / (width - paddingLeft - paddingRight))
            .coerceAtLeast(0)
            .coerceAtMost(columns - 1)
        val gy = ((vy - paddingTop) * rows / (height - paddingTop - paddingBottom))
            .coerceAtLeast(0)
            .coerceAtMost(rows - 1)
        return gx to gy
    }

    private fun gridToPopupCoords(gx: Int, gy: Int): Pair<Int, Int> {
        val (sx, sy) = IntArray(2).apply(::getLocationInWindow)
        val yoff = resources.displayMetrics.heightPixels + Utils.getStatusBarHeight(context) + Utils.getNavigationBarHeight(context) - (sy + height)
        val vx = calculateSideMargin() + gx * ((width - paddingLeft - paddingRight) / columns)
        val vy = paddingTop + gy * ((height - paddingTop - paddingBottom) / rows)
        return sx + vx to height - vy + yoff
    }

    private fun getIconBounds(x: Int, y: Int): Rect {
        val w = (width - paddingLeft - paddingRight) / columns
        val h = (height - paddingTop - paddingBottom) / rows
        val l = paddingLeft + w * x
        val t = paddingTop + h * y
        return Rect(l, t, w, h)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
            highlight = null
            invalidate()
        }
        return gestureListener.onTouchEvent(e)
    }

    private val gestureListener = GestureDetector(context, object : OnGestureListener {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) {
            highlight = viewToGridCoords(e.x.toInt(), e.y.toInt())
            invalidate()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) = false

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (abs(vy) > abs(vx) && vy > 0)
                Utils.pullStatusBar(context)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val (x, y) = viewToGridCoords(e.x.toInt(), e.y.toInt())
            val item = getItem(x, y) ?: return false
            item.open(this@PinnedGridView, getIconBounds(x, y))
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val (x, y) = viewToGridCoords(e.x.toInt(), e.y.toInt())
            val item = getItem(x, y) ?: return
            val (vx, vy) = gridToPopupCoords(x, y)
            LongPressMenu.popup(this@PinnedGridView, item, Gravity.BOTTOM or Gravity.START, vx, vy)
            Utils.vibrate(context)
            replacePreview = ItemPreview(NonDrawable, x, y)
            Utils.startDrag(this@PinnedGridView, item, x to y)
            setItem(x, y, null)
            showDropTargets = true
        }
    })

    override fun onDragEvent(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                val (x, y) = viewToGridCoords(event.x.toInt(), event.y.toInt())
                val replaceItem = (event.localState as? Pair<Int, Int>)?.takeIf { it.first is Int && it.second is Int }
                if (replaceItem == x to y)
                    LongPressMenu.dismissCurrent()
                val d = dropPreview
                if (d == null || d.x != x || d.y != y)
                    Utils.tick(context)
                val newItem = LauncherItem.decode(context, event.clipDescription.label.toString())!!
                dropPreview = ItemPreview(IconLoader.loadIcon(context, newItem), x, y)
                if (replaceItem != null) {
                    replacePreview = ItemPreview(
                        getItem(x, y)?.let { IconLoader.loadIcon(context, it) } ?: NonDrawable,
                        replaceItem.first, replaceItem.second
                    )
                }
                invalidate()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                Utils.tick(context)
                dropPreview = null
                val replaceItem = (event.localState as? Pair<Int, Int>)?.takeIf { it.first is Int && it.second is Int }
                if (replaceItem != null) {
                    replacePreview = ItemPreview(
                        NonDrawable,
                        replaceItem.first, replaceItem.second
                    )
                }
                invalidate()
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                dropPreview = null
                replacePreview = null
                showDropTargets = false
                LongPressMenu.onDragEnded()
            }
            DragEvent.ACTION_DROP -> {
                LiveWallpaper.drop(context, windowToken, event.x.toInt(), event.y.toInt())
                Utils.vibrate(context)

                val (x, y) = viewToGridCoords(event.x.toInt(), event.y.toInt())

                val origCoords = (event.localState as? Pair<Int, Int>)?.takeIf { it.first is Int && it.second is Int }
                if (origCoords != null)
                    setItem(origCoords.first, origCoords.second, getItem(x, y))

                val newItem = LauncherItem.decode(context, event.clipDescription.label.toString())!!
                setItem(x, y, newItem)
                invalidate()
            }
        }
        return true
    }
}