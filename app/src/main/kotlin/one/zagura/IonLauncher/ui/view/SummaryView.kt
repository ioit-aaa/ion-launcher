package one.zagura.IonLauncher.ui.view

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.OnDoubleTapListener
import android.view.MotionEvent
import android.view.View
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.summary.BatteryStatus
import one.zagura.IonLauncher.data.summary.Event
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.notification.TopNotificationProvider
import one.zagura.IonLauncher.provider.summary.Alarm
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.util.LiveWallpaper
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.SlideGestureHelper
import one.zagura.IonLauncher.util.Utils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

class SummaryView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
) : View(context) {

    var showAtAGlance = true
        set(value) {
            field = value
            invalidate()
        }

    private var topString: CharSequence = ""
    private var bottomString: CharSequence? = ""
    private var events = emptyArray<CompiledEvent>()
    private var isGlanceMultiline = false
    private var onGlanceTap: (() -> Unit)? = null

    fun updateAtAGlance() {
        // dependencies: notification, media, battery
        updateAtAGlanceInternal()
        val cd = if (bottomString == null) topString
        else "$topString\n$bottomString"
        post {
            contentDescription = cd
            val ax = drawCtx.radius / 3
            topString = TextUtils.ellipsize(
                topString,
                pureTitlePaint,
                width - paddingLeft - paddingRight - ax * 2,
                TextUtils.TruncateAt.END,
            )
            if (bottomString != null) {
                bottomString = TextUtils.ellipsize(
                    bottomString,
                    pureTextPaint,
                    width - paddingLeft - paddingRight - ax * 2,
                    TextUtils.TruncateAt.END,
                )
            }
            invalidate()
        }
    }

    fun updateEvents(events: List<Event>) {
        val alarm = Alarm.get(context)

        var i = 0
        val e = arrayOfNulls<CompiledEvent>(events.size + (alarm?.let { 1 } ?: 0))

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
        postInvalidate()
    }

    fun clearData() {
        events = emptyArray()
        topString = ""
        contentDescription = ""
        bottomString = null
    }

    private val pureTitlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            typeface = Typeface.create(null, 300, false)
    }
    private val pureTextPaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            Typeface.create(null, 500, false)
        else
            Typeface.DEFAULT_BOLD
    }
    private val rightTextPaint = Paint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.MONOSPACE
    }
    private val colorPaint = Paint().apply {
        isAntiAlias = true
    }

    private fun batteryStatusToString(status: BatteryStatus): String {
        return when (status) {
            BatteryStatus.Charged -> context.getString(R.string.fully_charged)
            is BatteryStatus.Charging -> context.getString(R.string.charging, status.level)
            is BatteryStatus.Discharging -> context.getString(R.string.discharging, status.level)
        }
    }

    private fun getDateString(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        else
            DateFormat.getMediumDateFormat(context).format(Calendar.getInstance().time)
    }

    private fun updateAtAGlanceInternal() {
        isGlanceMultiline = false

        val batteryStatus = Battery.getStatus(context)
        if (batteryStatus is BatteryStatus.Discharging && batteryStatus.level <= 20) {
            topString = getDateString()
            bottomString = batteryStatusToString(batteryStatus)
            onGlanceTap = ::openBatterySettings
            return
        }

        val notif = TopNotificationProvider.getResource()
        if (notif != null) {
            if (notif.subtitle == null) {
                topString = getDateString()
                bottomString = notif.title
                onGlanceTap = notif.open?.let {{ it.send() }}
            } else {
                topString = notif.title
                bottomString = notif.subtitle
                isGlanceMultiline = true
                onGlanceTap = notif.open?.let {{ it.send() }}
            }
            return
        }

        val media = NotificationService.MediaObserver.getResource()
        if (media.isNotEmpty()) {
            val item = media.firstOrNull { it.isPlaying }
            if (item != null) {
                topString = getDateString()
                bottomString = "${item.title} • ${item.subtitle}"
                onGlanceTap = { item.onTap(this) }
                return
            }
        }

        topString = getDateString()
        bottomString = batteryStatusToString(batteryStatus)
        onGlanceTap = ::openBatterySettings
    }

    override fun onDraw(canvas: Canvas) {
        val dp = resources.displayMetrics.density

        val separation = 6 * dp
        val padding = (14 * dp).coerceAtLeast(drawCtx.radius * 3 / 4)
        val dotRadius = 2 * dp

        var y = if (showAtAGlance) {
            var y = paddingTop - pureTitlePaint.ascent()
            val ax = drawCtx.radius / 3
            canvas.drawText(topString, 0, topString.length, paddingLeft + ax, y, pureTitlePaint)
            bottomString?.let {
                y += pureTitlePaint.descent() + separation - pureTextPaint.ascent()
                canvas.drawText(it, 0, it.length, paddingLeft + ax, y, pureTextPaint)
            }
            y
        } else 0f

        if (events.isEmpty())
            return

        y += paddingLeft

        val eventHeight = drawCtx.textPaint.descent() - drawCtx.textPaint.ascent()
        val eventRightHeight = rightTextPaint.descent() - rightTextPaint.ascent()

        val circXOffset = paddingLeft + padding
        val textXOffset = paddingLeft + padding + dotRadius * 2 + 6 * dp
        val roff = (eventHeight - eventRightHeight) / 2 - rightTextPaint.ascent()

        val totalHeight = eventHeight * events.size + separation * (events.size - 1) + padding * 2
        drawCtx.drawCard(dp, canvas, paddingLeft.toFloat(), y, (width - paddingRight).toFloat(), y + totalHeight)

        var bottomTop = y + padding
        for (event in events) {
            canvas.drawRoundRect(
                circXOffset,
                bottomTop,
                circXOffset + dotRadius * 2,
                bottomTop + eventHeight,
                dotRadius, dotRadius,
                colorPaint.apply { color = event.color ?: drawCtx.textPaint.color }
            )
            canvas.drawText(event.left, textXOffset, bottomTop - drawCtx.textPaint.ascent(), drawCtx.textPaint)
            if (event.right != null)
                canvas.drawText(event.right, width - paddingRight - padding, bottomTop + roff, rightTextPaint)
            bottomTop += eventHeight + separation
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        SlideGestureHelper.onTouchEvent(context, e)
        return gestureDetector.onTouchEvent(e)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.OnGestureListener, OnDoubleTapListener {
        override fun onDown(e: MotionEvent) = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onLongPress(e: MotionEvent) =
            Gestures.onLongPress(this@SummaryView, e.x.toInt(), e.y.toInt() + y.toInt())
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) =
            SlideGestureHelper.onScroll(context, e1, e2)
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float) =
            SlideGestureHelper.onFling(context, vx, vy)

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (tryConsumeTap(e)) return true
            LiveWallpaper.tap(context, windowToken, e.x.toInt(), e.y.toInt())
            return false
        }
        override fun onSingleTapConfirmed(e: MotionEvent) = false
        override fun onDoubleTap(e: MotionEvent) = false
        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            Gestures.onDoubleTap(context)
            return true
        }

        private fun tryConsumeTap(e: MotionEvent): Boolean {
            val dp = resources.displayMetrics.density
            val padding = 14 * dp
            val separation = 6 * dp

            val y = e.y
            val dateHeight = pureTitlePaint.descent() - pureTitlePaint.ascent()
            var dt = paddingTop + dateHeight + separation
            if (!isGlanceMultiline && y < dt)
                return performClick()
            val extraHeight = pureTextPaint.descent() - pureTextPaint.ascent()
            dt += extraHeight + paddingLeft / 2
            if (y < dt) {
                onGlanceTap?.invoke()
                return true
            }
            if (events.isEmpty())
                return false
            val x = e.x
            if (x < paddingLeft + padding || x > width - paddingRight - padding)
                return false

            dt += paddingLeft / 2 + padding
            val yy = y - dt
            if (yy < 0)
                return false
            val eventHeight = drawCtx.textPaint.descent() - drawCtx.textPaint.ascent() + separation
            val i = yy.toInt() / eventHeight.toInt()
            if (i >= events.size)
                return false
            events[i].open(this@SummaryView)
            return true
        }
    })

    private fun openBatterySettings() {
        context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), LauncherItem.createOpeningAnimation(this).toBundle())
    }

    override fun performClick(): Boolean {
        super.performClick()
        try {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_CALENDAR)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                LauncherItem.createOpeningAnimation(this).toBundle()
            )
        } catch (_: ActivityNotFoundException) {}
        return true
    }

    fun applyCustomizations(settings: Settings) {
        rightTextPaint.color = ColorThemer.cardHint(context)
        val fg = ColorThemer.wallForeground(context)
        pureTextPaint.color = fg
        pureTitlePaint.color = fg
        if (ColorThemer.lightness(fg) < 0.5) {
            pureTitlePaint.clearShadowLayer()
            pureTextPaint.clearShadowLayer()
        }
        else {
            pureTitlePaint.setShadowLayer(2f, 0f, 1f, 0x55000000)
            pureTextPaint.setShadowLayer(2f, 0f, 1f, 0x55000000)
        }
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
                try {
                    view.context.startActivity(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_APP_CALENDAR)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        LauncherItem.createOpeningAnimation(view).toBundle()
                    )
                } catch (_: ActivityNotFoundException) {}
            }
        }
    }
}