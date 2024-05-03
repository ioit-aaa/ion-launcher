package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.DragEvent
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.NonDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import kotlin.math.abs

class SuggestionRowView(
    context: Context,
    val showDropTargets: () -> Unit,
    val onSearch: () -> Unit,
) : View(context) {

    private var showSearchButton = false
    private var suggestions = emptyList<LauncherItem>()
    private var labels = emptyList<CharSequence>()

    fun update(allSuggestions: List<LauncherItem>) {
        context.ionApplication.task {
            suggestions = takeSuggestions(allSuggestions)
            if (suggestions.isEmpty()) post {
                labels = emptyList()
                isVisible = false
            }
            else post {
                updateLabels()
                isVisible = true
                invalidate()
            }
        }
    }

    fun applyCustomizations(settings: Settings) {
        showSearchButton = settings["layout:search-in-suggestions", false]
        pillPaint.color = ColorThemer.pillBackground(context)
        textPaint.color = ColorThemer.pillForeground(context)
        icSearch.setTint(ColorThemer.pillForeground(context))
        updateLabels()
        invalidate()
    }

    private val pillPaint = Paint().apply {

    }

    private val textPaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }

    private val icSearch = resources.getDrawable(R.drawable.ic_search)

    private val tmpRect = Rect()

    private val textHeight = run {
        textPaint.getTextBounds("X", 0, 1, tmpRect)
        tmpRect.height()
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - pl - pr
        val height = height - pt - pb
        val dp = resources.displayMetrics.density

        if (showSearchButton) {
            val x = pl + width - height
            val y = pt
            val r = height / 2f
            val p = (8 * dp).toInt()
            canvas.drawCircle(x + r, y + r, r, pillPaint)
            icSearch.setBounds(x + p, y + p, x + height - p, y + height - p)
            icSearch.draw(canvas)
        }

        val separation = 12 * dp
        val suggestionsWidth = if (showSearchButton) width - height.toFloat() else width + separation
        val singleWidth = suggestionsWidth / suggestions.size
        var x = pl.toFloat()
        val r = height * dp
        val p = (8 * dp).toInt()
        for (i in suggestions.indices) {
            val item = suggestions[i]
            val icon = IconLoader.loadIcon(context, item)
            canvas.drawRoundRect(x, pt.toFloat(), x + singleWidth - separation, pt + height.toFloat(), r, r, pillPaint)
            icon.copyBounds(tmpRect)
            icon.setBounds(x.toInt() + p, pt + p, x.toInt() + height - p, pt + height - p)
            icon.draw(canvas)
            icon.bounds = tmpRect
            val textX = x + height - p / 2
            val text = labels[i]
            canvas.drawText(text, 0, text.length, textX, pt + (height + textHeight) / 2f, textPaint)
            x += singleWidth
        }
    }

    override fun onTouchEvent(e: MotionEvent) = gestureListener.onTouchEvent(e)

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
            if (showSearchButton && e.x > width - paddingRight - (height - paddingTop - paddingBottom)) {
                onSearch()
                return true
            }
            val i = (e.x.toInt() - paddingLeft) * suggestions.size / (width - paddingLeft - paddingRight)
            if (i < 0 || i >= suggestions.size)
                return false
            suggestions[i].open(this@SuggestionRowView, run {
                val w = (width - paddingLeft - paddingRight) / suggestions.size
                val h = (height - paddingTop - paddingBottom)
                val x = paddingLeft + w * i
                Rect(x, paddingTop, w, h)
            })
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val i = (e.x.toInt() - paddingLeft) * suggestions.size / (width - paddingLeft - paddingRight)
            if (i < 0 || i >= suggestions.size)
                return
            val item = suggestions[i]
            val dp = resources.displayMetrics.density
            val separation = 12 * dp
            val xOff = paddingLeft + (if (showSearchButton)
                (width - paddingLeft - paddingRight - (height - paddingTop - paddingBottom))
            else
                (width - paddingLeft - paddingRight + separation.toInt())) * i / suggestions.size
            LongPressMenu.popup(
                this@SuggestionRowView, item,
                Gravity.BOTTOM or Gravity.START,
                xOff,
                height + Utils.getNavigationBarHeight(context) + (4 * dp).toInt()
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dp = resources.displayMetrics.density
        val height = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(height, heightMeasureSpec)
        )
    }

    private fun updateLabels() {
        val dp = resources.displayMetrics.density
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        val separation = 12 * dp
        val suggestionsWidth =
            if (showSearchButton) width - height.toFloat()
            else width + separation
        val w = suggestionsWidth / suggestions.size - separation - height
        labels = suggestions.map {
            TextUtils.ellipsize(it.label, textPaint, w, TextUtils.TruncateAt.END)
        }
    }

    private fun takeSuggestions(allSuggestions: List<LauncherItem>): List<LauncherItem> {
        val suggestionCount = context.ionApplication.settings["suggestion:count", 3]
        if (suggestionCount == 0)
            return emptyList()
        val dockItems = Dock.getItems(context)
        val suggestions = ArrayList<LauncherItem>(suggestionCount)
        for (s in allSuggestions) {
            if (dockItems.contains(s))
                continue
            suggestions.add(s)
            if (suggestions.size == suggestionCount)
                break
        }
        return suggestions
    }
}