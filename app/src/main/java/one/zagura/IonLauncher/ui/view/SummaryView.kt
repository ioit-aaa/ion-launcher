package one.zagura.IonLauncher.ui.view

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.summary.Event
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.summary.Alarm
import one.zagura.IonLauncher.util.LiveWallpaper
import one.zagura.IonLauncher.util.Utils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import kotlin.math.abs


class SummaryView(c: Context) : View(c) {

    private var dateString = ""
    private var events = emptyList<CompiledEvent>()

    private val gestureDetector = GestureDetector(context, object : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent) = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onLongPress(e: MotionEvent) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) = false

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (tryConsumeTap(e)) return true
            LiveWallpaper.tap(context, windowToken, e.x.toInt(), e.y.toInt())
            return false
        }

        private fun tryConsumeTap(e: MotionEvent): Boolean {
            val y = e.y
            val dateHeight = datePaint.descent() - datePaint.ascent()
            if (y < paddingTop + paddingLeft + dateHeight)
                return performClick()
            if (events.isEmpty())
                return false
            val dp = resources.displayMetrics.density
            val x = e.x
            val innerPadding = 40 * dp
            if (x < innerPadding || x > width - innerPadding)
                return false

            val m = 6 * dp
            val titleHeight = titlePaint.descent() - titlePaint.ascent() + m
            val eventHeight = textPaint.descent() - textPaint.ascent() + m
            val contentHeight = titleHeight + events.size * eventHeight
            val dt = dateHeight + paddingTop + paddingLeft
            val yOffset = dt + (height - dt - paddingBottom - contentHeight) / 2
            val yy = y - yOffset
            if (yy < 0 || yy > contentHeight)
                return false

            if (yy < titleHeight)
                return performClick()

            val i = (yy.toInt() - titleHeight.toInt()) / eventHeight.toInt()
            if (i >= events.size)
                return false
            events[i].open(this@SummaryView)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (abs(velocityY) > abs(velocityX) && velocityY > 0)
                Utils.pullStatusBar(context)
            return true
        }
    })

    fun updateEvents(events: List<Event>) {
        dateString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        } else
            DateFormat.getMediumDateFormat(context)
                .format(Calendar.getInstance().time)
        val e = ArrayList<CompiledEvent>()
        Alarm.get(context)?.let {
            e.add(CompiledEvent.Alarm(
                it.open,
                context.getString(R.string.upcoming_alarm),
                Utils.format(context, it.time),
            ))
        }
        events.mapTo(e) {
            val textRight = if (it.allDay) null else {
                val begin = Utils.format(context, it.begin)
                val end = Utils.format(context, it.end)
                "$begin - $end"
            }
            CompiledEvent.Calendar(it.title, textRight, it.color)
        }
        this.events = e
        contentDescription = dateString
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawText(dateString, paddingLeft.toFloat(), paddingTop - datePaint.ascent(), datePaint)
        if (events.isEmpty())
            return
        val dp = resources.displayMetrics.density
        val dateHeight = datePaint.descent() - datePaint.ascent()
        val titleHeight = titlePaint.descent() - titlePaint.ascent()
        val eventHeight = textPaint.descent() - textPaint.ascent()
        val eventRightHeight = rightTextPaint.descent() - rightTextPaint.ascent()
        val m = 6 * dp
        val contentHeight = titleHeight + events.size * (eventHeight + m)
        val dt = dateHeight + paddingTop + paddingLeft
        val yOffset = dt + (height - dt - paddingBottom - contentHeight) / 2
        val innerPadding = 40 * dp
        canvas.drawText(resources.getString(R.string.today), innerPadding, yOffset - titlePaint.ascent(), titlePaint)
        var bottomTop = yOffset + titleHeight + m
        val radius = 2 * dp
        val circXOffset = innerPadding + radius + 4 * dp
        val textXOffset = innerPadding + radius * 2 + 12 * dp
        val roff = (eventHeight - eventRightHeight) / 2 - rightTextPaint.ascent()
        for (event in events) {
            canvas.drawCircle(
                circXOffset,
                bottomTop + eventHeight / 2f,
                radius,
                colorPaint.apply { color = event.color ?: textPaint.color }
            )
            canvas.drawText(event.left, textXOffset, bottomTop - textPaint.ascent(), textPaint)
            if (event.right != null)
                canvas.drawText(event.right, width - innerPadding, bottomTop + roff, rightTextPaint)
            bottomTop += eventHeight + m
        }
    }

    private val titlePaint = Paint().apply {
        textSize = 14 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val datePaint = Paint().apply {
        textSize = 14 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }
    private val textPaint = Paint().apply {
        textSize = 14 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }
    private val rightTextPaint = Paint().apply {
        textSize = 12 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.MONOSPACE
    }
    private val colorPaint = Paint().apply {
        isAntiAlias = true
    }

    override fun onTouchEvent(e: MotionEvent) = gestureDetector.onTouchEvent(e)

    override fun performClick(): Boolean {
        super.performClick()
        context.startActivity(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), LauncherItem.createOpeningAnimation(this))
        return true
    }

    fun applyCustomizations() {
        val textColor = ColorThemer.foreground(context)
        val hintColor = ColorThemer.hint(context)
        titlePaint.color = textColor
        textPaint.color = textColor
        datePaint.color = hintColor
        rightTextPaint.color = hintColor
    }

    private sealed class CompiledEvent(
        val left: String,
        val right: String?,
        val color: Int?,
    ) {
        abstract fun open(view: View)

        class Alarm(
            val open: PendingIntent,
            left: String,
            right: String?,
        ) : CompiledEvent(left, right, null) {
            override fun open(view: View) = open.send()
        }

        class Calendar(
            left: String,
            right: String?,
            color: Int,
        ) : CompiledEvent(left, right, color) {
            override fun open(view: View) {
                view.context.startActivity(Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_CALENDAR)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), LauncherItem.createOpeningAnimation(view))
            }
        }
    }
}