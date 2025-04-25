package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.view.DragEvent
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import androidx.core.graphics.record
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.iconify.IconifyAnim
import one.zagura.IonLauncher.util.LiveWallpaper
import one.zagura.IonLauncher.util.drawable.NonDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.SlideGestureHelper
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils

@SuppressLint("UseCompatLoadingForDrawables")
class PinnedGridView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
) : View(context) {

    class ItemPreview(val icon: Drawable, val x: Int, val y: Int)

    var showDropTargets
        get() = gridPic != null
        set(value) {
            gridPic = if (value)
                Picture().record(width, height) {
                    drawGrid()
                }
            else null
            redrawItems()
        }
    private var gridPic: Picture? = null
    private var iconPics = emptyArray<Picture?>()

    // also used with a NonDrawable for iconify animation
    private var replacePreview: ItemPreview? = null
    private var dropPreview: ItemPreview? = null
    private var showPress: Pair<Int, Int>? = null

    private var items = ArrayList<LauncherItem?>()
    private var columns = 0
    private var rows = 0

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

    fun applyLayoutCustomizations(settings: Settings) {
        columns = settings["dock:columns", 5]
        rows = settings["dock:rows", 2]
        iconPics = arrayOfNulls(columns * rows)
        isVisible = rows != 0
        updateLayoutParams {
            height = calculateGridHeight()
        }
        TaskRunner.submit {
            updateGridApps(false)
            post(::redrawItems)
        }
    }

    /**
     * Called after [applyLayoutCustomizations] if necessary
     */
    fun calculateSideMargin(): Int {
        val w = resources.displayMetrics.widthPixels - paddingLeft - paddingRight
        return (paddingLeft + (w - drawCtx.iconSize * columns) / (columns + 1)).toInt()
    }

    companion object {
        fun calculateSideMargin(context: Context): Int {
            val w = context.resources.displayMetrics.widthPixels
            val dp = context.resources.displayMetrics.density
            val settings = context.ionApplication.settings
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
            val columns = settings["dock:columns", 5]
            return (w - iconSize * columns) / (columns + 1)
        }
    }

    fun calculateGridHeight(): Int {
        val iconSize = drawCtx.iconSize
        val h = iconSize * rows + calculateSideMargin() * rows
        return paddingBottom + paddingTop + h.toInt()
    }

    @AnyThread
    fun updateGridApps(redraw: Boolean = true) {
        var newLen = 0
        Dock.getItems(context) { i, it ->
            newLen++
            when {
                i >= items.size -> items.add(it)
                items[i] == it -> return@getItems
                else -> items[i] = it
            }
            val gridX = i % columns
            val gridY = i / columns
            if (!redraw)
                redrawItem(gridX, gridY)
        }
        while (items.size > newLen)
            items.removeAt(items.lastIndex)
    }

    override fun onDraw(canvas: Canvas) {
        for (pic in iconPics)
            pic?.draw(canvas)
        gridPic?.draw(canvas)
    }

    private fun redrawItems() {
        val r = drawCtx.iconSize / 2
        val sr = drawCtx.iconSize * 0.9f / 2
        val br = drawCtx.iconSize * 3 / 4
        val ww = width - paddingLeft - paddingRight
        val l = paddingLeft + (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = width - l * 2
        val h = height
        for (x in 0 until columns)
            for (y in 0 until rows) {
                iconPics[x + y * columns] = drawSingleItem(x, y, br, sr, r, l, w, h)
                continue
            }
        invalidate()
    }
    private fun redrawItem(x: Int, y: Int) {
        val r = drawCtx.iconSize / 2
        val sr = drawCtx.iconSize * 0.9f / 2
        val br = drawCtx.iconSize * 3 / 4
        val ww = width - paddingLeft - paddingRight
        val l = paddingLeft + (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = width - l * 2
        val h = height
        iconPics[x + y * columns] = drawSingleItem(x, y, br, sr, r, l, w, h)
        invalidate()
    }

    private fun drawSingleItem(
        x: Int,
        y: Int,
        br: Float,
        sr: Float,
        r: Float,
        l: Float,
        w: Float,
        h: Int,
    ): Picture? {
        var r1 = r
        val r = if (showPress?.let { it.first == x && it.second == y } == true)
            br else if (showDropTargets) sr else r1
        val icon = dropPreview?.takeIf { it.x == x && it.y == y }?.icon
            ?: replacePreview?.takeIf { it.x == x && it.y == y }?.icon
            ?: IconLoader.loadIcon(context, getItem(x, y) ?: return null)
        val centerX = l + w * (0.5f + x) / columns
        val centerY = h * (0.5f + y) / rows
        icon.copyBounds(drawCtx.tmpRect)
        icon.setBounds(
            (centerX - r).toInt(),
            (centerY - r).toInt(),
            (centerX + r).toInt(),
            (centerY + r).toInt()
        )
        val pic = Picture().record(width, height) {
            if (showDropTargets) {
                val a = icon.alpha
                icon.alpha = 200
                icon.draw(this)
                icon.alpha = a
            } else
                icon.draw(this)
        }
        icon.bounds = drawCtx.tmpRect
        return pic
    }

    private fun Canvas.drawGrid() {
        val fg = ColorThemer.wallForeground(context)

        val gridPaint = Paint().apply {
            isAntiAlias = true
            color = fg and 0xffffff or 0xdd000000.toInt()
        }

        val targetPaint = Paint().apply {
            val dp = resources.displayMetrics.density
            strokeWidth = 2 * dp
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = fg and 0xffffff or 0x88000000.toInt()
        }

        val dp = resources.displayMetrics.density
        val r = drawCtx.iconSize / 2f + dp

        val ww = width - paddingLeft - paddingRight
        val l = paddingLeft + (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = width - l * 2
        val h = height

        for (x in 0 ..< columns)
            for (y in 0 ..< rows) {
                val x = l + w / columns * (0.5f + x) - r
                val y = h / rows * (0.5f + y) - r
                drawRoundRect(
                    x, y, x + r * 2, y + r * 2,
                    drawCtx.radius + dp * 2, drawCtx.radius + dp * 2,
                    targetPaint
                )
            }
        for (x in 0 .. columns)
            for (y in 0 .. rows) {
                drawCircle(
                    l + w / columns * x.toFloat(),
                    h / rows * y.toFloat(),
                    dp, gridPaint)
            }
    }

    private fun viewToGridCoords(vx: Int, vy: Int): Pair<Int, Int> {
        val ww = width - paddingLeft - paddingRight
        val l = (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = ww - l * 2
        val h = height - paddingTop - paddingBottom

        val gx = ((vx - paddingLeft - l) * columns / w).toInt()
            .coerceAtLeast(0)
            .coerceAtMost(columns - 1)
        val gy = (vy * rows / h)
            .coerceAtLeast(0)
            .coerceAtMost(rows - 1)
        return gx to gy
    }

    private fun gridToPopupCoords(gx: Int, gy: Int): Pair<Int, Int> {
        val ww = width - paddingLeft - paddingRight
        val l = (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = ww - l * 2
        val (sx, sy) = IntArray(2).apply(::getLocationInWindow)
        val vx = paddingLeft + l * 2 + gx * (w / columns)
        val vy = gy * (height / rows)
        return (sx + vx).toInt() to (sy + vy + l).toInt()
    }

    fun getIconBounds(x: Int, y: Int): Rect {
        val ww = width - paddingLeft - paddingRight
        val l = ((ww - drawCtx.iconSize * columns) / (columns + 1) / 2).toInt()
        val w = ww - l * 2

        val rh = height / rows
        val x = l * 2 + x * w / columns
        val y = l + y * rh
        return Rect(x, y, drawCtx.iconSize.toInt(), drawCtx.iconSize.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @AnyThread
    fun prepareIconifyAnim(packageName: String, user: UserHandle): IconifyAnim? {
        val i = items.indexOfFirst { it is App && it.packageName == packageName && it.userHandle == user }
        if (i == -1 || rows == 0)
            return null

        val gridX = i % columns
        val gridY = i / columns
        replacePreview = ItemPreview(NonDrawable, gridX, gridY)
        post { redrawItem(gridX, gridY) }

        val ww = width - paddingLeft - paddingRight
        val l = (ww - drawCtx.iconSize * columns) / (columns + 1) / 2
        val w = ww - l * 2

        val rh = height / rows.toFloat()
        val x = l * 2 + gridX * w / columns
        val y = l + gridY * rh + y // instead of IntArray(2).apply(::getLocationOnScreen)[1], which doesn't work well with animations
        return IconifyAnim(
            items[i]!!,
            RectF(x, y, x + drawCtx.iconSize, y + drawCtx.iconSize),
        ) {
            replacePreview = null
            redrawItem(gridX, gridY)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        SlideGestureHelper.onTouchEvent(context, e)
        if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
            val tmp = showPress
            showPress = null
            tmp?.let { redrawItem(it.first, it.second) }
        }
        return gestureListener.onTouchEvent(e)
    }

    private val gestureListener = GestureDetector(context, object : OnGestureListener {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) {
            val tmp = showPress
            showPress = viewToGridCoords(e.x.toInt(), e.y.toInt())
            tmp?.let { redrawItem(it.first, it.second) }
            showPress?.let { redrawItem(it.first, it.second) }
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) =
            SlideGestureHelper.onScroll(context, e1, e2)
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float) =
            SlideGestureHelper.onFling(context, vx, vy)

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
            LongPressMenu.popupIcon(this@PinnedGridView, item, vx, vy, drawCtx.iconSize.toInt(), LongPressMenu.Where.DOCK)
            Utils.click(context)
            replacePreview = ItemPreview(NonDrawable, x, y)
            dropPreview = ItemPreview(NonDrawable, x, y)
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
                if (replaceItem != x to y)
                    LongPressMenu.dismissCurrent()
                val d = dropPreview
                val newItem = LauncherItem.decode(context, event.clipDescription.label.toString())!!
                dropPreview = ItemPreview(IconLoader.loadIcon(context, newItem), x, y)
                if (d == null || d.x != x || d.y != y) {
                    Utils.tick(context)
                    d?.let { redrawItem(it.x, it.y) }
                    dropPreview?.let { redrawItem(x, y) }
                }
                if (replaceItem != null) {
                    replacePreview = ItemPreview(
                        getItem(x, y)?.let { IconLoader.loadIcon(context, it) } ?: NonDrawable,
                        replaceItem.first, replaceItem.second
                    )
                    redrawItem(replaceItem.first, replaceItem.second)
                }
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                Utils.tick(context)
                val tmp = dropPreview
                dropPreview = null
                tmp?.let { redrawItem(it.x, it.y) }
                val replaceItem = (event.localState as? Pair<Int, Int>)?.takeIf { it.first is Int && it.second is Int }
                if (replaceItem != null) {
                    replacePreview = ItemPreview(
                        NonDrawable,
                        replaceItem.first, replaceItem.second
                    )
                    redrawItem(replaceItem.first, replaceItem.second)
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                dropPreview = null
                replacePreview = null
                showDropTargets = false // this will redraw all items
                LongPressMenu.onDragEnded()
                items.trimToSize()
            }
            DragEvent.ACTION_DROP -> {
                LiveWallpaper.drop(context, windowToken, event.x.toInt(), event.y.toInt())
                Utils.vibrateDrop(context)

                val (x, y) = viewToGridCoords(event.x.toInt(), event.y.toInt())

                val origCoords = (event.localState as? Pair<Int, Int>)?.takeIf { it.first is Int && it.second is Int }
                if (origCoords != null)
                    setItem(origCoords.first, origCoords.second, getItem(x, y))

                val newItem = LauncherItem.decode(context, event.clipDescription.label.toString())!!
                setItem(x, y, newItem)
            }
        }
        return true
    }
}