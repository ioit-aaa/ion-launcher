package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
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

class MediaView(
    context: Context,
    private val drawCtx: SharedDrawingContext,
) : View(context) {

    class PreparedMediaData(
        val icon: Drawable?,
        var title: CharSequence,
        var subtitle: CharSequence,
        var isPlaying: Boolean,
        val data: MediaPlayerData,
    )

    private var players = emptyArray<PreparedMediaData>()

    private var separation = 0f

    private val icPlay = resources.getDrawable(R.drawable.ic_play)
    private val icPause = resources.getDrawable(R.drawable.ic_pause)
    private val icTrackNext = resources.getDrawable(R.drawable.ic_track_next)

    fun update(players: Array<MediaPlayerData>) {
        TaskRunner.submit {
            this.players = Array(players.size) {
                val player = players[it]
                val drawable = player.cover?.let {
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
                PreparedMediaData(drawable, "", "", player.isPlaying?.invoke() == true, player)
            }
            post {
                requestLayout()
                invalidate()
            }
        }
    }

    fun clearData() {
        players = emptyArray()
    }

    fun applyCustomizations(settings: Settings) {
        val dp = resources.displayMetrics.density
        separation = 12 * dp
        val c = ColorThemer.cardForeground(context)
        icPlay.setTint(c)
        icPause.setTint(c)
        icTrackNext.setTint(c)
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        val width = width - pl - pr
        val dp = resources.displayMetrics.density

        var y = pt.toFloat()
        val iconPadding = (6 * dp).toInt()
        val controlPadding = (10 * dp).toInt()
        for (i in players.indices) {
            val player = players[i]
            val icon = player.icon
            canvas.drawRoundRect(pl.toFloat(), y, pl + width.toFloat(), y + drawCtx.iconSize, drawCtx.radius, drawCtx.radius, drawCtx.cardPaint)
            if (icon != null) {
                icon.copyBounds(drawCtx.tmpRect)
                icon.setBounds(pl + iconPadding, y.toInt() + iconPadding, pl + drawCtx.iconSize.toInt() - iconPadding, y.toInt() + drawCtx.iconSize.toInt() - iconPadding)
                icon.draw(canvas)
                icon.bounds = drawCtx.tmpRect
            }
            val textX = pl + drawCtx.iconSize
            val s = 3 * dp
            canvas.drawText(player.title, 0, player.title.length, textX, y + drawCtx.iconSize / 2f - s, drawCtx.titlePaint)
            canvas.drawText(player.subtitle, 0, player.subtitle.length, textX, y + drawCtx.iconSize / 2f + s + drawCtx.textHeight, drawCtx.subtitlePaint)

            val playIcon = if (player.isPlaying) icPause else icPlay
            val lastIconOff = (2 * dp).toInt()

            if (player.data.next == null) {
                val controlX = width - drawCtx.iconSize.toInt() - lastIconOff
                drawIcon(canvas, playIcon, controlX, y.toInt(), controlPadding)
            } else {
                var controlX = width - drawCtx.iconSize.toInt() * 2
                drawIcon(canvas, playIcon, controlX, y.toInt(), controlPadding)
                controlX += drawCtx.iconSize.toInt() - lastIconOff
                drawIcon(canvas, icTrackNext, controlX, y.toInt(), controlPadding)
            }

            y += drawCtx.iconSize + separation
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable, x: Int, y: Int, controlPadding: Int) {
        icon.setBounds(
            x + controlPadding,
            y + controlPadding,
            x + drawCtx.iconSize.toInt() - controlPadding,
            y + drawCtx.iconSize.toInt() - controlPadding
        )
        icon.draw(canvas)
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
            var x = drawCtx.iconSize
            player.data.next?.let {
                if (e.x >= width - paddingRight - x) {
                    Utils.click(context)
                    it()
                    return true
                }
                x += drawCtx.iconSize
            }
            if (e.x >= width - paddingRight - x) {
                Utils.click(context)
                if (player.data.isPlaying?.invoke() == true) {
                    player.data.pause()
                    player.isPlaying = false
                } else {
                    player.data.play()
                    player.isPlaying = true
                }
                invalidate()
            }
            else player.data.onTap?.send()
            return true
        }
    })

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (player in players) {
            val avail = (width - paddingLeft - paddingRight) - drawCtx.iconSize * 3
            val title = TextUtils.ellipsize(player.data.title, drawCtx.titlePaint, avail, TextUtils.TruncateAt.END)
            val subtitle = TextUtils.ellipsize(player.data.subtitle, drawCtx.subtitlePaint, avail, TextUtils.TruncateAt.END)
            player.title = title
            player.subtitle = subtitle
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = drawCtx.iconSize.toInt() * players.size + separation.toInt() * (players.size - 1).coerceAtLeast(0)
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(height, MeasureSpec.UNSPECIFIED)
        )
    }
}