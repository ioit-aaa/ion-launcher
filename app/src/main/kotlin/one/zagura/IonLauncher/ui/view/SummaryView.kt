package one.zagura.IonLauncher.ui.view

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.CallLog
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.summary.Event
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.summary.Alarm
import one.zagura.IonLauncher.provider.summary.MissedCalls
import one.zagura.IonLauncher.util.LiveWallpaper
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sign


class SummaryView(
    context: Context,
    private val drawingContext: SharedDrawingContext,
) : View(context) {

    private var dateString = ""
    private var events = emptyArray<CompiledEvent>()

    private val titlePaint = Paint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val textPaint = Paint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }
    private val rightTextPaint = Paint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.MONOSPACE
    }
    private val cardPaint = Paint().apply { isAntiAlias = true }
    private val colorPaint = Paint().apply {
        isAntiAlias = true
    }

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
            val dp = resources.displayMetrics.density
            val padding = 16 * dp
            val separation = 6 * dp

            val y = e.y
            val dateHeight = titlePaint.descent() - titlePaint.ascent()
            val dt = paddingTop + padding + dateHeight + separation
            if (y < dt)
                return performClick()
            if (events.isEmpty())
                return false
            val x = e.x
            if (x < paddingLeft + padding || x > width - paddingRight - padding)
                return false

            val eventHeight = textPaint.descent() - textPaint.ascent() + separation
            val yy = y - dt
            if (yy < 0)
                return false
            val i = yy.toInt() / eventHeight.toInt()
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
        dateString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        else
            DateFormat.getMediumDateFormat(context).format(Calendar.getInstance().time)

        val missedCalls = MissedCalls.get(context, Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -2)
        }.timeInMillis)
        val alarm = Alarm.get(context)

        var i = 0
        val e = arrayOfNulls<CompiledEvent>(events.size + (alarm?.let { 1 } ?: 0) + missedCalls.sign)

        if (missedCalls != 0)
            e[i++] = CompiledEvent.MissedCalls(context, missedCalls)
        alarm?.let {
            e[i++] = CompiledEvent.Alarm(
                it.open,
                context.getString(R.string.upcoming_alarm),
                Utils.format(context, it.time),
            )
        }
        for (event in events) {
            val textRight = if (event.allDay) null else {
                val begin = Utils.format(context, event.begin)
                val end = Utils.format(context, event.end)
                "$begin - $end"
            }
            e[i++] = CompiledEvent.Calendar(event.title, textRight, event.color)
        }
        this.events = e as Array<CompiledEvent>
        post {
            contentDescription = dateString
            invalidate()
        }
    }

    fun clearData() {
        events = emptyArray()
        dateString = ""
        contentDescription = ""
    }

    override fun onDraw(canvas: Canvas) {
        if (events.isEmpty()) {
            canvas.drawText(dateString, paddingLeft.toFloat(), paddingTop - titlePaint.ascent(), titlePaint)
            return
        }
        val dp = resources.displayMetrics.density

        val titleHeight = titlePaint.descent() - titlePaint.ascent()
        val eventHeight = textPaint.descent() - textPaint.ascent()
        val eventRightHeight = rightTextPaint.descent() - rightTextPaint.ascent()

        val separation = 6 * dp
        val padding = 16 * dp
        val dotRadius = 2 * dp
        val circXOffset = paddingLeft + padding + dotRadius + 4 * dp
        val textXOffset = paddingLeft + padding + dotRadius * 2 + 12 * dp
        val roff = (eventHeight - eventRightHeight) / 2 - rightTextPaint.ascent()

        val totalHeight = titleHeight + (eventHeight + separation) * events.size + padding * 2
        canvas.drawRoundRect(paddingLeft.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(), paddingTop + totalHeight, drawingContext.radius, drawingContext.radius, cardPaint)
        canvas.drawText(dateString, paddingLeft.toFloat() + padding, paddingTop + padding - titlePaint.ascent(), titlePaint)

        var bottomTop = paddingTop + padding + titleHeight + separation
        for (event in events) {
            canvas.drawCircle(
                circXOffset,
                bottomTop + eventHeight / 2f,
                dotRadius,
                colorPaint.apply { color = event.color ?: textPaint.color }
            )
            canvas.drawText(event.left, textXOffset, bottomTop - textPaint.ascent(), textPaint)
            if (event.right != null)
                canvas.drawText(event.right, width - paddingRight - padding, bottomTop + roff, rightTextPaint)
            bottomTop += eventHeight + separation
        }
    }

    override fun onTouchEvent(e: MotionEvent) = gestureDetector.onTouchEvent(e)

    override fun performClick(): Boolean {
        super.performClick()
        context.startActivity(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), LauncherItem.createOpeningAnimation(this))
        return true
    }

    fun applyCustomizations(settings: Settings) {
        val textColor = ColorThemer.foreground(context)
        val hintColor = ColorThemer.hint(context)
        titlePaint.color = textColor
        textPaint.color = textColor
        rightTextPaint.color = hintColor
        cardPaint.color = ColorThemer.backgroundToday(context)
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

        class MissedCalls(
            context: Context,
            missed: Int,
        ) : CompiledEvent(context.resources.getQuantityString(R.plurals.missed_calls, missed, missed), null, null) {
            override fun open(view: View) {
                view.context.startActivity(Intent(Intent.ACTION_VIEW)
                    .setType(CallLog.Calls.CONTENT_TYPE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), LauncherItem.createOpeningAnimation(view))
            }
        }
    }
}