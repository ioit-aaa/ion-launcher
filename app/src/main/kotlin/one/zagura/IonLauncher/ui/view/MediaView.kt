package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.ClippedDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils
import kotlin.math.abs
import kotlin.math.min

class MediaView(context: Context) : View(context) {

    class PreparedMediaData(
        val icon: Drawable?,
        var title: CharSequence,
        var subtitle: CharSequence,
        var isPlaying: Boolean,
        val data: MediaPlayerData,
    )

    private var players = emptyList<PreparedMediaData>()

    fun update(players: List<MediaPlayerData>) {
        TaskRunner.submit {
            this.players = players.map {
                val drawable = it.cover?.let {
                    val bitmap = when {
                        it.width > it.height -> Bitmap.createBitmap(it, (it.width - it.height) / 2, 0, it.height, it.height)
                        it.width < it.height -> Bitmap.createBitmap(it, 0, (it.height - it.width) / 2, it.width, it.width)
                        else -> it
                    }
                    val drawable = BitmapDrawable(bitmap)
                    drawable.setBounds(0, 0, bitmap.width, bitmap.height)
                    ClippedDrawable(drawable, Path().apply {
                        val r = bitmap.width / 2f
                        addCircle(r, r, r, Path.Direction.CW)
                    })
                }
                PreparedMediaData(drawable, "", "", it.isPlaying?.invoke() == true, it)
            }
            post {
                requestLayout()
                invalidate()
            }
        }
    }

    fun applyCustomizations(settings: Settings) {
        val dp = resources.displayMetrics.density
        separation = 12 * dp
        itemHeight = settings["dock:icon-size", 48] * dp
        val c = ColorThemer.background(context)
        pillPaint.color = ColorThemer.foreground(context)
        titlePaint.color = c
        subtitlePaint.color = ColorThemer.reverseHint(context)
        icPlay.setTint(c)
        icPause.setTint(c)
        icTrackNext.setTint(c)
        requestLayout()
        invalidate()
    }

    private var itemHeight = 0f
    private var separation = 0f

    private val tmpRect = Rect()

    private val icPlay = resources.getDrawable(R.drawable.ic_play)
    private val icPause = resources.getDrawable(R.drawable.ic_pause)
    private val icTrackNext = resources.getDrawable(R.drawable.ic_track_next)

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - pl - pr
        val dp = resources.displayMetrics.density

        var y = pt.toFloat()
        val r = itemHeight
        val iconPadding = (6 * dp).toInt()
        val controlPadding = (10 * dp).toInt()
        for (i in players.indices) {
            val player = players[i]
            val icon = player.icon
            canvas.drawRoundRect(pl.toFloat(), y, pl + width.toFloat(), y + itemHeight, r, r, pillPaint)
            if (icon != null) {
                icon.copyBounds(tmpRect)
                icon.setBounds(pl + iconPadding, y.toInt() + iconPadding, pl + itemHeight.toInt() - iconPadding, y.toInt() + itemHeight.toInt() - iconPadding)
                icon.draw(canvas)
                icon.bounds = tmpRect
            }
            val textX = pl + itemHeight
            val s = 3 * dp
            canvas.drawText(player.title, 0, player.title.length, textX, y + itemHeight / 2f - s, titlePaint)
            canvas.drawText(player.subtitle, 0, player.subtitle.length, textX, y + itemHeight / 2f + s + textHeight, subtitlePaint)

            var controlX = width - itemHeight.toInt() * 2
            val playIcon = if (player.isPlaying) icPause else icPlay
            playIcon.setBounds(
                controlX + controlPadding,
                y.toInt() + controlPadding,
                controlX + itemHeight.toInt() - controlPadding,
                y.toInt() + itemHeight.toInt() - controlPadding
            )
            playIcon.draw(canvas)

            controlX += itemHeight.toInt()
            icTrackNext.setBounds(controlX + controlPadding, y.toInt() + controlPadding, controlX + itemHeight.toInt() - controlPadding, y.toInt() + itemHeight.toInt() - controlPadding)
            icTrackNext.draw(canvas)

            y += itemHeight + separation
        }
    }

    private val pillPaint = Paint().apply {

    }

    private val titlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subtitlePaint = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        isSubpixelText = true
    }

    private val textHeight = run {
        subtitlePaint.getTextBounds("X", 0, 1, tmpRect)
        tmpRect.height()
    }

    override fun onTouchEvent(e: MotionEvent) = gestureListener.onTouchEvent(e)

    private val gestureListener = GestureDetector(context, object : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent) = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) = false
        override fun onLongPress(e: MotionEvent) {}

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (abs(vy) > abs(vx) && vy > 0)
                Utils.pullStatusBar(context)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val i = (e.y.toInt() - paddingTop) * players.size / (height - paddingTop - paddingBottom)
            if (i < 0 || i >= players.size)
                return false
            val player = players[i]
            if (e.x < width - paddingRight - itemHeight * 2)
                player.data.onTap?.send()
            else if (e.x < width - paddingRight - itemHeight) {
                Utils.click(context)
                if (player.data.isPlaying?.invoke() == true) {
                    player.data.pause()
                    player.isPlaying = false
                } else {
                    player.data.play()
                    player.isPlaying = true
                }
                invalidate()
            } else
                player.data.next()
            return true
        }
    })

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (player in players) {
            val avail = (width - paddingLeft - paddingRight) - itemHeight * 3
            val title = TextUtils.ellipsize(player.data.title, titlePaint, avail, TextUtils.TruncateAt.END)
            val subtitle = TextUtils.ellipsize(player.data.subtitle, subtitlePaint, avail, TextUtils.TruncateAt.END)
            player.title = title
            player.subtitle = subtitle
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = itemHeight.toInt() * players.size + separation.toInt() * (players.size - 1).coerceAtLeast(0)
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(height, MeasureSpec.UNSPECIFIED)
        )
    }
}