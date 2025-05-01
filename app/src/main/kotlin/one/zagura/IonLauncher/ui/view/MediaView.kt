package one.zagura.IonLauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.IconThemer
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.SlideGestureHelper
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils

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

    private val icPlay = resources.getDrawable(R.drawable.play)
    private val icPause = resources.getDrawable(R.drawable.pause)
    private val icTrackNext = resources.getDrawable(R.drawable.track_next)

    private var fgColor = 0

    fun update(players: Array<MediaPlayerData>) {
        if (players.isEmpty()) {
            this.players = emptyArray()
            isVisible = false
            return
        }
        isVisible = true
        TaskRunner.submit {
            this.players = Array(players.size) {
                val player = players[it]
                val drawable = player.cover?.let {
                    IconThemer.fromQuadImage(BitmapDrawable(it).apply {
                        setBounds(0, 0, it.width, it.height)
                    })
                } ?: AppLoader.getResource().firstOrNull { it.packageName == player.controller.packageName }
                    ?.let { IconLoader.loadIcon(context, it) }
                PreparedMediaData(drawable, "", "", player.isPlaying, player)
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
        fgColor = ColorThemer.cardForeground(context)
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val w = width - pl - pr
        val dp = resources.displayMetrics.density

        var y = pt.toFloat()
        val controlPadding = (10 * dp).coerceAtLeast((drawCtx.iconSize - dp * 42) / 2f).toInt()
        for (i in players.indices) {
            val player = players[i]
            val icon = player.icon

            if (player.data.color == 0)
                drawCtx.drawCard(dp, canvas, pl.toFloat(), y, pl + w.toFloat(), y + drawCtx.iconSize)
            else {
                val c = drawCtx.cardPaint.color
                drawCtx.cardPaint.color = player.data.color
                drawCtx.drawCard(dp, canvas, pl.toFloat(), y, pl + w.toFloat(), y + drawCtx.iconSize)
                drawCtx.cardPaint.color = c
            }

            val textX = if (icon == null) 4 * dp else {
                icon.copyBounds(drawCtx.tmpRect)
                canvas.save()
                canvas.translate(pl.toFloat(), y)
                icon.setBounds(0, 0, drawCtx.iconSize.toInt(), drawCtx.iconSize.toInt())
                icon.draw(canvas)
                canvas.restore()
                icon.bounds = drawCtx.tmpRect
                drawCtx.iconSize
            } + pl + 8 * dp
            val s = 3 * dp
            if (player.data.textColor == 0) {
                canvas.drawText(player.title, 0, player.title.length, textX, y + drawCtx.iconSize / 2f - s, drawCtx.titlePaint)
                canvas.drawText(player.subtitle, 0, player.subtitle.length, textX, y + drawCtx.iconSize / 2f + s + drawCtx.textHeight, drawCtx.subtitlePaint)
            } else {
                val h = drawCtx.titlePaint.color
                val b = drawCtx.subtitlePaint.color
                drawCtx.titlePaint.color = player.data.textColor
                drawCtx.subtitlePaint.color = (player.data.textColor and 0xffffff) or 0xaa000000.toInt()
                canvas.drawText(player.title, 0, player.title.length, textX, y + drawCtx.iconSize / 2f - s, drawCtx.titlePaint)
                canvas.drawText(player.subtitle, 0, player.subtitle.length, textX, y + drawCtx.iconSize / 2f + s + drawCtx.textHeight, drawCtx.subtitlePaint)
                drawCtx.titlePaint.color = h
                drawCtx.subtitlePaint.color = b
            }

            val playIcon = if (player.isPlaying) icPause else icPlay
            val lastIconOff = (2 * dp).toInt()

            val c = if (player.data.color != 0) player.data.textColor else fgColor
            playIcon.setTint(c)
            if (!player.data.hasNext()) {
                val controlX = width - paddingRight - drawCtx.iconSize.toInt() - lastIconOff
                drawIcon(canvas, playIcon, controlX, y.toInt(), controlPadding)
            } else {
                icTrackNext.setTint(c)
                var controlX = width - paddingRight - drawCtx.iconSize.toInt() * 2
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

    override fun onTouchEvent(e: MotionEvent): Boolean {
        SlideGestureHelper.onTouchEvent(context, e)
        return gestureListener.onTouchEvent(e)
    }

    private val gestureListener = GestureDetector(context, object : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent) = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float) =
            SlideGestureHelper.onScroll(context, e1, e2)
        override fun onLongPress(e: MotionEvent) =
            Gestures.onLongPress(this@MediaView, e.x.toInt(), e.y.toInt() + y.toInt())

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float) =
            SlideGestureHelper.onFling(context, vx, vy)

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (players.isEmpty())
                return false
            val i = (e.y.toInt() - paddingTop) * players.size / (height - paddingTop - paddingBottom)
            if (i < 0 || i >= players.size)
                return false
            val player = players[i]
            var x = drawCtx.iconSize
            if (player.data.hasNext()) {
                if (e.x >= width - paddingRight - x) {
                    Utils.click(context)
                    player.data.next()
                    return true
                }
                x += drawCtx.iconSize
            }
            if (e.x >= width - paddingRight - x) {
                Utils.click(context)
                if (player.data.isPlaying) {
                    player.data.pause()
                    player.isPlaying = false
                } else {
                    player.data.play()
                    player.isPlaying = true
                }
                invalidate()
            }
            else player.data.onTap(this@MediaView)
            return true
        }
    })

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (player in players) {
            val avail = (width - paddingLeft - paddingRight) - drawCtx.iconSize * if (player.data.hasNext()) 3 else 2
            val title = TextUtils.ellipsize(player.data.title, drawCtx.titlePaint, avail, TextUtils.TruncateAt.END)
            val subtitle = TextUtils.ellipsize(player.data.subtitle, drawCtx.subtitlePaint, avail, TextUtils.TruncateAt.END)
            player.title = title
            player.subtitle = subtitle
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = drawCtx.iconSize.toInt() * players.size + separation.toInt() * (players.size - 1).coerceAtLeast(0) + paddingTop + paddingBottom
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(height, MeasureSpec.UNSPECIFIED)
        )
    }
}