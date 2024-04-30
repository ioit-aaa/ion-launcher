package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.media.AudioManager
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.ClippedDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import kotlin.math.min

class MusicView(c: Context) : LinearLayout(c) {

    private val musicService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val image = ImageView(context).apply {
        val dp = context.resources.displayMetrics.density
        scaleType = ImageView.ScaleType.CENTER_CROP
        val p = (6 * dp).toInt()
        setPadding(p, p, p, p)
    }

    private val title = TextView(context).apply {
        textSize = 14f
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subtitle = TextView(context).apply {
        textSize = 14f
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
    }

    private val play = ImageView(context).apply {
        contentDescription = resources.getString(R.string.play)
        setOnClickListener {
            it as ImageView
            Utils.click(it.context)
            if (musicService.isMusicActive) {
                musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                it.setImageResource(R.drawable.ic_play)
                it.contentDescription = resources.getString(R.string.play)
            } else {
                musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                it.setImageResource(R.drawable.ic_pause)
                it.contentDescription = resources.getString(R.string.pause)
            }
        }
        val dp = context.resources.displayMetrics.density
        val p = (10 * dp).toInt()
        setPadding(p, p, p, p)
    }

    private val next = ImageView(context).apply {
        setImageResource(R.drawable.ic_track_next)
        setOnClickListener {
            Utils.click(it.context)
            musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
            musicService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            play.setImageResource(R.drawable.ic_pause)
        }
        val dp = context.resources.displayMetrics.density
        val p = (10 * dp).toInt()
        setPadding(p, p, p, p)
        contentDescription = resources.getString(R.string.next_track)
    }

    init {
        val dp = resources.displayMetrics.density

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val r = 99 * dp
        background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))

        addView(image)

        val textLayout = LinearLayout(context).apply {
            this.orientation = VERTICAL

            addView(title, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(subtitle, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        addView(textLayout, LayoutParams(0, WRAP_CONTENT, 1f))
        addView(play, LayoutParams((36 * dp).toInt(), MATCH_PARENT).apply {
            leftMargin = (10 * dp).toInt()
        })
        addView(next, LayoutParams((36 * dp).toInt(), MATCH_PARENT).apply {
            rightMargin = (4 * dp).toInt()
        })

        isVisible = false
    }

    @SuppressLint("SetTextI18n")
    fun updateTrack(data: MediaPlayerData?) {
        if (data == null) {
            isVisible = false
            return
        }
        isVisible = true

        setOnClickListener { data.onTap?.send() }
        if (data.cover == null)
            image.isVisible = false
        else {
            image.isVisible = true
            val drawable = BitmapDrawable(data.cover)
            drawable.setBounds(0, 0, data.cover.width, data.cover.height)
            val path = Path().apply {
                val w = data.cover.width / 2f
                val h = data.cover.height / 2f
                addCircle(w, h, min(w, h), Path.Direction.CW)
            }
            image.setImageDrawable(ClippedDrawable(drawable, path))
        }
        with(title) {
            text = data.title
        }
        with(subtitle) {
            isVisible = data.subtitle != null
            text = data.subtitle
        }

        updatePlayButton()
    }

    fun applyCustomizations(settings: Settings) {
        val dp = resources.displayMetrics.density
        val bgColor = ColorThemer.foreground(context)
        val titleColor = ColorThemer.background(context)
        val textColor = ColorThemer.reverseHint(context)
        backgroundTintList = ColorStateList.valueOf(bgColor)
        title.setTextColor(titleColor)
        subtitle.setTextColor(textColor)
        play.imageTintList = ColorStateList.valueOf(titleColor)
        next.imageTintList = ColorStateList.valueOf(titleColor)
        val s = (settings["dock:icon-size", 48] * dp).toInt()
        image.updateLayoutParams {
            width = s
            height = s
        }
        play.updateLayoutParams {
            width = s
            height = s
        }
        next.updateLayoutParams {
            width = s
            height = s
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updatePlayButton()
    }

    private fun updatePlayButton() {
        if (musicService.isMusicActive) {
            play.setImageResource(R.drawable.ic_pause)
            play.contentDescription = resources.getString(R.string.pause)
        } else {
            play.setImageResource(R.drawable.ic_play)
            play.contentDescription = resources.getString(R.string.play)
        }
    }
}