package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Picture
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
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import androidx.core.graphics.record
import androidx.core.graphics.withSave
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
import one.zagura.IonLauncher.util.SlideGestureHelper
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils

class SuggestionRowView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
    private val showDropTargets: () -> Unit,
    private val onSearch: () -> Unit,
    private val onExtraButton: (SuggestionRowView) -> Unit,
) : View(context) {

    var showCross = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    private var showSearchButton = false
    private var showLabels = false
    private var pillShape = false
    private var suggestions = emptyList<LauncherItem>()
    private var labels = emptyArray<CharSequence>()
    private var hideI = -1

    private val suggestionsPic = Picture()

    private var transitionToSearch = 0f

    private val icSearch = resources.getDrawable(R.drawable.search)
    private val icCross = resources.getDrawable(R.drawable.cross)
    private val icOptions = resources.getDrawable(R.drawable.options)

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
                redrawSuggestions()
                isVisible = true
            }
        }
    }

    fun applyCustomizations(settings: Settings) {
        showSearchButton = settings["layout:search-in-suggestions", true]
        showLabels = settings["suggestion:labels", false]
        pillShape = settings["suggestion:pill", false]
        val fg = ColorThemer.cardForeground(context)
        icSearch.setTint(fg)
        icCross.setTint(fg)
        icOptions.setTint(fg)
        if (showLabels && suggestions.isNotEmpty())
            updateLabels()
        else labels = emptyArray()
        redrawSuggestions()
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - pl - pr
        val height = height - pt - pb
        val dp = resources.displayMetrics.density

        if (!pillShape || (!showSearchButton && transitionToSearch == 1f))
            drawCtx.drawCard(dp, canvas,
                pl.toFloat(),
                pt.toFloat(),
                pl + width.toFloat(),
                pt + height.toFloat())
        else if (!showSearchButton && transitionToSearch != 0f) {
            canvas.saveLayerAlpha(pl.toFloat(), pt.toFloat(), pl + width.toFloat(), pt + height.toFloat(), (transitionToSearch * 255).toInt())
            drawCtx.drawCard(dp, canvas,
                pl.toFloat(),
                pt.toFloat(),
                pl + width.toFloat(),
                pt + height.toFloat())
            canvas.restore()
        }

        val p = (10 * dp).toInt()
        if (showSearchButton) {
            val x = pl + ((width - height) * (1f - transitionToSearch)).toInt()
            if (pillShape)
                drawCtx.drawCard(dp, canvas,
                    x.toFloat(),
                    pt.toFloat(),
                    pl + width.toFloat(),
                    pt + height.toFloat())
            icSearch.setBounds(x + p, pt + p, x + height - p, pt + height - p)
            icSearch.draw(canvas)
        } else if (transitionToSearch != 0f) {
            icSearch.setBounds(pl + p, pt + p, pl + height - p, pt + height - p)
            icSearch.alpha = (transitionToSearch * 255).toInt()
            icSearch.draw(canvas)
            icSearch.alpha = 255
        }
        val rightIcon = if (showCross) icCross else icOptions
        val x = pl + width - height
        rightIcon.setBounds(x + p, pt + p, x + height - p, pt + height - p)
        rightIcon.alpha = (transitionToSearch * 255).toInt()
        rightIcon.draw(canvas)

        canvas.withSave {
            val yo = paddingTop.toFloat() + drawCtx.iconSize / 2
            canvas.translate(paddingLeft.toFloat(), yo)
            val s = 1f - transitionToSearch
            canvas.scale(s, s)
            canvas.translate(0f, -yo)
            suggestionsPic.draw(canvas)
        }
    }

    private fun redrawSuggestions() {
        suggestionsPic.record(width, height) {
            drawSuggestions(this)
        }
        invalidate()
    }
    private fun drawSuggestions(canvas: Canvas) {
        if (suggestions.isEmpty())
            return
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - paddingLeft - pr
        val height = height - pt - pb
        val dp = resources.displayMetrics.density
        val suggestionsWidth = if (showSearchButton) width - height else width
        if (!showLabels) {
            val spacing = 8 * dp
            val o = if (pillShape) 0 else spacing.toInt()
            val cellSize = (height - o).coerceAtMost(suggestionsWidth / suggestions.size)

            val contentHeight = cellSize - spacing.toInt()
            var x = 0f
            val t = pt + (height - contentHeight) / 2

            for (i in suggestions.indices) {
                if (i == hideI) {
                    x += cellSize
                    continue
                }
                drawItemIcon(canvas, suggestions[i],
                    (x + o).toInt(),
                    t.toInt(),
                    (x + o + contentHeight).toInt(),
                    (t + contentHeight).toInt())
                x += cellSize
            }
            return
        }
        if (pillShape) { // and show labels
            val spacing = (8 * dp).toInt()
            val iconPadding = (8 * dp).toInt()
            val singleWidth = (if (showSearchButton) suggestionsWidth - spacing else suggestionsWidth + spacing) / suggestions.size

            val contentHeight = height - (18 * dp).toInt()
            var x = 0f
            val t = pt + (height - contentHeight) / 2

            for (i in suggestions.indices) {
                if (i == hideI) {
                    x += singleWidth
                    continue
                }
                drawCtx.drawCard(dp, canvas,
                    x,
                    t.toFloat(),
                    x + singleWidth - spacing,
                    t + contentHeight.toFloat())
                drawItemIcon(canvas, suggestions[i],
                    x.toInt() + iconPadding,
                    t.toInt() + iconPadding,
                    (x + contentHeight).toInt() - iconPadding,
                    (t + contentHeight).toInt() - iconPadding)
                val textX = x + contentHeight
                val text = labels[i]
                canvas.drawText(text, 0, text.length, textX, pt + (height + drawCtx.textHeight) / 2f, drawCtx.textPaint)
                x += singleWidth
            }
            return
        }
        // not pill-shaped and show labels
        val singleWidth = suggestionsWidth.toFloat() / suggestions.size
        val iconPadding = 8 * dp

        var x = 0f
        val t = pt

        for (i in suggestions.indices) {
            if (i == hideI) {
                x += singleWidth
                continue
            }
            drawItemIcon(canvas, suggestions[i],
                (x + iconPadding).toInt(),
                (t + iconPadding).toInt(),
                (x + height - iconPadding).toInt(),
                (t + height - iconPadding).toInt())
            val textX = x + height
            val text = labels[i]
            canvas.drawText(text, 0, text.length, textX, pt + (height + drawCtx.textHeight) / 2f, drawCtx.textPaint)
            x += singleWidth
        }
    }

    fun drawItemIcon(canvas: Canvas, item: LauncherItem, x1: Int, y1: Int, x2: Int, y2: Int) {
        if (x1 >= x2 || y1 >= y2)
            return
        val icon = IconLoader.loadIcon(context, item)
        icon.copyBounds(drawCtx.tmpRect)
        icon.setBounds(x1, y1, x2, y2)
        icon.draw(canvas)
        icon.bounds = drawCtx.tmpRect
    }

    override fun onTouchEvent(e: MotionEvent) = gestureListener.onTouchEvent(e)

    @RequiresApi(Build.VERSION_CODES.Q)
    @AnyThread
    fun prepareIconifyAnim(packageName: String, user: UserHandle): IconifyAnim? {
        val i = suggestions.indexOfFirst { it is App && it.packageName == packageName && it.userHandle == user }
        if (i == -1)
            return null

        hideI = i
        post { redrawSuggestions() }

        val dp = resources.displayMetrics.density
        val iconPadding = 8 * dp
        val x = iToX(i).toFloat()
        val y = paddingTop + iconPadding + IntArray(2).apply(::getLocationOnScreen)[1]
        val s = height - paddingTop - paddingBottom - iconPadding * 2
        return IconifyAnim(suggestions[i], RectF(x, y, x + s, y + s)) {
            hideI = -1
            redrawSuggestions()
        }
    }

    fun transitionToSearchBarHolder(f: Float) {
        transitionToSearch = f
        invalidate()
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

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float) =
            SlideGestureHelper.onFling(context, vx, vy)

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (transitionToSearch == 1f) {
                if (e.x > width - drawCtx.iconSize - paddingRight) {
                    onExtraButton(this@SuggestionRowView)
                    return true
                }
                return false
            }
            val i = xToI(e.x)
            if (i < 0 || i >= suggestions.size) {
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
            if (transitionToSearch != 0f)
                return
            val i = xToI(e.x)
            if (i < 0 || i >= suggestions.size) {
                Gestures.onLongPress(this@SuggestionRowView, e.x.toInt(), e.y.toInt() + y.toInt())
                return
            }
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
        redrawSuggestions()
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