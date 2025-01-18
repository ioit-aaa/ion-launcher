package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.UserHandle
import android.text.TextUtils
import android.view.DragEvent
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.iconify.IconifyAnim
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils
import kotlin.math.abs

class SuggestionRowView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
    private val showDropTargets: () -> Unit,
    private val onSearch: () -> Unit,
) : View(context) {

    private var showSearchButton = false
    private var showLabels = false
    private var pillShape = false
    private var suggestions = emptyList<LauncherItem>()
    private var labels = emptyArray<CharSequence>()
    private var hideI = -1

    private val icSearch = resources.getDrawable(R.drawable.search)

    fun update(allSuggestions: List<LauncherItem>) {
        TaskRunner.submit {
            val newSuggestions = takeSuggestions(allSuggestions)
            if (newSuggestions.isEmpty()) post {
                suggestions = emptyList()
                labels = emptyArray()
                isVisible = showSearchButton
            }
            else post {
                suggestions = newSuggestions
                if (showLabels)
                    updateLabels()
                invalidate()
                isVisible = true
            }
        }
    }

    fun applyCustomizations(settings: Settings) {
        showSearchButton = settings["layout:search-in-suggestions", true]
        showLabels = settings["suggestion:labels", false]
        pillShape = settings["suggestion:pill", false]
        icSearch.setTint(ColorThemer.cardForeground(context))
        if (showLabels && suggestions.isNotEmpty())
            updateLabels()
        else labels = emptyArray()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - pl - pr
        val height = height - pt - pb
        val dp = resources.displayMetrics.density

        if (!pillShape)
            drawCtx.drawCard(dp, canvas,
                pl.toFloat(),
                pt.toFloat(),
                pl + width.toFloat(),
                pt + height.toFloat())

        if (showSearchButton) {
            val x = pl + width - height
            val p = (10 * dp).toInt()
            if (pillShape)
                drawCtx.drawCard(dp, canvas,
                    x.toFloat(),
                    pt.toFloat(),
                    pl + width.toFloat(),
                    pt + height.toFloat())
            icSearch.setBounds(x + p, pt + p, x + height - p, pt + height - p)
            icSearch.draw(canvas)
        }
        if (suggestions.isEmpty())
            return

        val suggestionsWidth = if (showSearchButton) width - height else width
        if (!showLabels) {
            val spacing = 8 * dp
            val o = if (pillShape) 0 else spacing.toInt()
            val cellSize = (height - o).coerceAtMost(suggestionsWidth / suggestions.size)
            val singleWidth = cellSize - spacing.toInt()
            var x = pl
            val t = pt + (height - singleWidth) / 2
            for (i in suggestions.indices) {
                if (i == hideI) {
                    x += cellSize
                    continue
                }
                val item = suggestions[i]
                val icon = IconLoader.loadIcon(context, item)
                icon.copyBounds(drawCtx.tmpRect)
                icon.setBounds(
                    x + o,
                    t,
                    x + o + singleWidth,
                    t + singleWidth)
                icon.draw(canvas)
                icon.bounds = drawCtx.tmpRect
                x += cellSize
            }
            return
        }
        if (pillShape) {
            val spacing = (8 * dp).toInt()
            val iconPadding = (8 * dp).toInt()
            val pillHeight = height - (18 * dp).toInt()
            val singleWidth = (if (showSearchButton) suggestionsWidth - spacing else suggestionsWidth + spacing) / suggestions.size
            var x = pl.toFloat()
            val t = pt + (height - pillHeight) / 2
            for (i in suggestions.indices) {
                if (i == hideI) {
                    x += singleWidth
                    continue
                }
                drawCtx.drawCard(dp, canvas,
                    x,
                    t.toFloat(),
                    x + singleWidth - spacing,
                    t + pillHeight.toFloat())
                val item = suggestions[i]
                val icon = IconLoader.loadIcon(context, item)
                icon.copyBounds(drawCtx.tmpRect)
                icon.setBounds(
                    x.toInt() + iconPadding,
                    t + iconPadding,
                    (x + pillHeight).toInt() - iconPadding,
                    t + pillHeight - iconPadding)
                icon.draw(canvas)
                icon.bounds = drawCtx.tmpRect
                val textX = x + pillHeight
                val text = labels[i]
                canvas.drawText(text, 0, text.length, textX, pt + (height + drawCtx.textHeight) / 2f, drawCtx.textPaint)
                x += singleWidth
            }
            return
        }
        val singleWidth = suggestionsWidth.toFloat() / suggestions.size
        var x = pl.toFloat()
        val iconPadding = 8 * dp
        for (i in suggestions.indices) {
            if (i == hideI) {
                x += singleWidth
                continue
            }
            val item = suggestions[i]
            val icon = IconLoader.loadIcon(context, item)
            icon.copyBounds(drawCtx.tmpRect)
            icon.setBounds(
                (x + iconPadding).toInt(),
                (pt + iconPadding).toInt(),
                (x + height - iconPadding).toInt(),
                (pt + height - iconPadding).toInt())
            icon.draw(canvas)
            icon.bounds = drawCtx.tmpRect
            val textX = x + height
            val text = labels[i]
            canvas.drawText(text, 0, text.length, textX, pt + (height + drawCtx.textHeight) / 2f, drawCtx.textPaint)
            x += singleWidth
        }
    }

    override fun onTouchEvent(e: MotionEvent) = gestureListener.onTouchEvent(e)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun prepareIconifyAnim(packageName: String, user: UserHandle): IconifyAnim? {
        val i = suggestions.indexOfFirst { it is App && it.packageName == packageName && it.userHandle == user }
        if (i == -1)
            return null

        hideI = i
        invalidate()

        val dp = resources.displayMetrics.density
        val iconPadding = 8 * dp
        val x = iToX(i).toFloat()
        val y = paddingTop + iconPadding + IntArray(2).apply(::getLocationOnScreen)[1]
        val s = height - paddingTop - paddingBottom - iconPadding * 2
        return IconifyAnim(suggestions[i], RectF(x, y, x + s, y + s)) {
            hideI = -1
            invalidate()
        }
    }

    private fun xToI(x: Float): Int {
        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        val suggestionsWidth = if (showSearchButton) w - h else w
        val xi = x.toInt() - paddingLeft
        return if (showLabels)
            xi * suggestions.size / suggestionsWidth
        else {
            val dp = resources.displayMetrics.density
            val iconPadding = (8 * dp).toInt()
            (xi - iconPadding / 2) / (h - iconPadding).coerceAtMost(suggestionsWidth / suggestions.size.also { if (it == 0) return -1 })
        }
    }

    private fun iToX(i: Int): Int {
        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        val suggestionsWidth = if (showSearchButton) w - h else w
        return paddingLeft + if (showLabels)
            suggestionsWidth * i / suggestions.size
        else {
            val dp = resources.displayMetrics.density
            val iconPadding = (8 * dp).toInt()
            val o = if (pillShape) 0 else iconPadding
            o + i * (h - iconPadding).coerceAtMost(suggestionsWidth / suggestions.size.also { if (it == 0) return o })
        }
    }

    private val gestureListener = GestureDetector(context, object : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent) = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) = false

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (abs(vy) > abs(vx) && vy > 0)
                Utils.pullStatusBar(context)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val i = xToI(e.x)
            if (i < 0)
                return false
            if (i >= suggestions.size) {
                if (showSearchButton) {
                    onSearch()
                    return true
                }
                return false
            }
            suggestions[i].open(this@SuggestionRowView, run {
                val w = (width - paddingLeft - paddingRight) / suggestions.size
                val h = (height - paddingTop - paddingBottom)
                val x = paddingLeft + w * i
                Rect(x, paddingTop, w, h)
            })
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val i = xToI(e.x)
            if (i < 0 || i >= suggestions.size)
                return
            val item = suggestions[i]
            val dp = resources.displayMetrics.density
            val xOff = iToX(i)
            LongPressMenu.popup(
                this@SuggestionRowView, item,
                Gravity.BOTTOM or Gravity.START,
                xOff,
                (height - paddingTop),// + Utils.getNavigationBarHeight(context).coerceAtLeast(paddingLeft) + (8 * dp).toInt(),
                LongPressMenu.Where.SUGGESTION,
            )
            Utils.click(context)
            Utils.startDrag(this@SuggestionRowView, item, this@SuggestionRowView)
            showDropTargets()
        }
    })

    override fun onDragEvent(e: DragEvent): Boolean {
        if (e.action == DragEvent.ACTION_DRAG_EXITED) {
            if (e.localState == this)
                LongPressMenu.dismissCurrent()
        }
        return super.onDragEvent(e)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (suggestions.isEmpty())
            return
        if (showLabels)
            updateLabels()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dp = resources.displayMetrics.density
        val height = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(height, heightMeasureSpec)
        )
    }

    private fun updateLabels() {
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        val suggestionsWidth =
            if (showSearchButton) width - height
            else width
        val w = suggestionsWidth / suggestions.size - height.toFloat()
        labels = Array(suggestions.size) {
            TextUtils.ellipsize(LabelLoader.loadLabel(context, suggestions[it]), drawCtx.textPaint, w, TextUtils.TruncateAt.END)
        }
    }

    private fun takeSuggestions(allSuggestions: List<LauncherItem>): List<LauncherItem> {
        val suggestionCount = context.ionApplication.settings["suggestion:count", 4]
        if (suggestionCount == 0)
            return emptyList()
        return allSuggestions.take(suggestionCount)
    }
}