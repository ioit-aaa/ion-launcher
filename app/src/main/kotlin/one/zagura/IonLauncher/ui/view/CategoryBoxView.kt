package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.items.AppCategorizer
import one.zagura.IonLauncher.util.Utils

@SuppressLint("ViewConstructor")
class CategoryBoxView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
    onItemOpened: (App) -> Unit,
) : View(context) {

    companion object {
        const val GRID_SIZE = 2

        fun getNameForCategory(context: Context, category: AppCategorizer.AppCategory): String =
            context.getString(when (category) {
                AppCategorizer.AppCategory.AllApps -> R.string.all_apps
                AppCategorizer.AppCategory.System -> R.string.system
                AppCategorizer.AppCategory.Customization -> R.string.customization
                AppCategorizer.AppCategory.Utilities -> R.string.utilities
                AppCategorizer.AppCategory.Media -> R.string.media
                AppCategorizer.AppCategory.Communication -> R.string.communication
                AppCategorizer.AppCategory.Productivity -> R.string.productivity
                AppCategorizer.AppCategory.Wellbeing -> R.string.wellbeing
                AppCategorizer.AppCategory.Commute -> R.string.commute
                AppCategorizer.AppCategory.Games -> R.string.games
                AppCategorizer.AppCategory.Audio -> R.string.audio
                AppCategorizer.AppCategory.Image -> R.string.image
                AppCategorizer.AppCategory.News -> R.string.news
            })
    }

    var category = AppCategorizer.AppCategory.AllApps
        set(value) {
            field = value
            label = getNameForCategory(context, value)
            invalidate()
        }
    var apps = emptyList<App>()
        set(value) {
            field = value
            invalidate()
        }

    private var label = ""
    private val openIcon = resources.getDrawable(R.drawable.arrow_right)

    init {
        val p = (12 * resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val dp = resources.displayMetrics.density

        val iconCount = if (apps.size <= GRID_SIZE * GRID_SIZE) apps.size
        else GRID_SIZE * GRID_SIZE - 1
        val l = pl + ((width - pl - pr) - drawCtx.iconSize * GRID_SIZE) / (GRID_SIZE + 1) / 2
        drawCtx.drawCard(dp, canvas,
            pl.toFloat(),
            pt.toFloat(),
            width - pr.toFloat(),
            width - pr.toFloat(),
            r = if (drawCtx.radius == 0f) 0f else drawCtx.radius + l)

        with(drawCtx.textPaint) {
            textAlign = Paint.Align.CENTER
            canvas.drawText(label, width / 2f, width - pr.toFloat() + drawCtx.textHeight * 2, this)
            textAlign = Paint.Align.LEFT
        }

        for (i in 0 until iconCount) {
            val icon = IconLoader.loadIcon(context, apps[i])
            val x = i % GRID_SIZE
            val y = i / GRID_SIZE
            val centerX = l + (width - l * 2) * (0.5f + x) / GRID_SIZE
            val centerY = l + (width - l * 2) * (0.5f + y) / GRID_SIZE
            icon.copyBounds(drawCtx.tmpRect)
            val r = drawCtx.iconSize / 2f
            icon.setBounds((centerX - r).toInt(), (centerY - r).toInt(), (centerX + r).toInt(), (centerY + r).toInt())
            icon.draw(canvas)
            icon.bounds = drawCtx.tmpRect
        }
        if (iconCount < apps.size) {
            val centerX = l + (width - l * 2) * (0.5f + GRID_SIZE - 1) / GRID_SIZE
            val centerY = l + (width - l * 2) * (0.5f + GRID_SIZE - 1) / GRID_SIZE
            openIcon.copyBounds(drawCtx.tmpRect)
            val r = drawCtx.iconSize / 2f
            openIcon.setBounds((centerX - r).toInt(), (centerY - r).toInt(), (centerX + r).toInt(), (centerY + r).toInt())
            openIcon.draw(canvas)
            openIcon.bounds = drawCtx.tmpRect
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(MeasureSpec.getSize(widthMeasureSpec) + drawCtx.textHeight * 2, MeasureSpec.UNSPECIFIED)
        )
    }

    private fun viewToGridCoords(vx: Int, vy: Int): Pair<Int, Int> {
        val ww = width - paddingLeft - paddingRight
        val l = (ww - drawCtx.iconSize * GRID_SIZE) / (GRID_SIZE + 1) / 2
        val w = ww - l * 2

        val gx = ((vx - paddingLeft - l) * GRID_SIZE / w).toInt()
            .coerceAtLeast(0)
            .coerceAtMost(GRID_SIZE - 1)
        val gy = ((vy - paddingTop - l) * GRID_SIZE / w).toInt()
            .coerceAtLeast(0)
            .coerceAtMost(GRID_SIZE - 1)
        return gx to gy
    }

    private fun getApp(x: Int, y: Int): App? {
        val i = y * GRID_SIZE + x
        if (i >= apps.size)
            return null
        if (apps.size == GRID_SIZE * GRID_SIZE)
            return apps[i]
        if (i < GRID_SIZE * GRID_SIZE - 1)
            return apps[i]
        return null
    }

    fun getIconBounds(x: Int, y: Int): Rect {
        val ww = width - paddingLeft - paddingRight
        val l = ((ww - drawCtx.iconSize * GRID_SIZE) / (GRID_SIZE + 1) / 2).toInt()
        val w = ww - l * 2
        val x = l * 2 + x * w / GRID_SIZE
        val y = l * 2 + y * w / GRID_SIZE
        return Rect(x, y, drawCtx.iconSize.toInt(), drawCtx.iconSize.toInt())
    }

    override fun onTouchEvent(e: MotionEvent) = gestureListener.onTouchEvent(e)

    private var generator: (CategoryBoxView) -> Any? = { null }
    fun setDragState(generator: (CategoryBoxView) -> Any?) {
        this.generator = generator
    }

    private val gestureListener = GestureDetector(context, object : OnGestureListener {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) {}

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) = false
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float) = false

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val (x, y) = viewToGridCoords(e.x.toInt(), e.y.toInt())
            val item = getApp(x, y) ?: return performClick()
            item.open(this@CategoryBoxView, getIconBounds(x, y))
            onItemOpened(item)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val (x, y) = viewToGridCoords(e.x.toInt(), e.y.toInt())
            val item = getApp(x, y) ?: return
            Utils.click(context)

            val ww = width - paddingLeft - paddingRight
            val l = (ww - drawCtx.iconSize * GRID_SIZE) / (GRID_SIZE + 1) / 2
            val w = ww - l * 2
            val dp = resources.displayMetrics.density

            var vx = paddingLeft + l * 2 + x * (w / GRID_SIZE)
            var vy = paddingTop + l * 2 + y * (w / GRID_SIZE) - 4 * dp
            val (sx, sy) = IntArray(2).apply(::getLocationInWindow)
            vx += sx
            vy += sy
            val sh = resources.displayMetrics.heightPixels
            if (vy > sh / 2) {
                vy = sh +
                    Utils.getStatusBarHeight(context) +
                    Utils.getNavigationBarHeight(context) - vy
                LongPressMenu.popup(this@CategoryBoxView, item, Gravity.BOTTOM or Gravity.START, vx.toInt(), vy.toInt(), LongPressMenu.Where.DRAWER)
            }
            else
                LongPressMenu.popup(this@CategoryBoxView, item, Gravity.TOP or Gravity.START, vx.toInt(), (vy + drawCtx.iconSize + l).toInt(), LongPressMenu.Where.DRAWER)

            Utils.startDrag(this@CategoryBoxView, item, generator(this@CategoryBoxView))
        }
    })
}