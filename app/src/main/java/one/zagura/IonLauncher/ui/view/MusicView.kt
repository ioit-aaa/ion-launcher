package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
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
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.util.Utils

class MusicView(c: Context) : LinearLayout(c) {

    private val musicService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val image = ImageView(context)

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
        setImageResource(R.drawable.ic_play)
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
        val p = (4 * dp).toInt()
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
        val p = (4 * dp).toInt()
        setPadding(p, p, p, p)
        contentDescription = resources.getString(R.string.next_track)
    }

    private val controls = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val dp = context.resources.displayMetrics.density

        val textLayout = LinearLayout(context).apply {
            this.orientation = VERTICAL

            addView(title, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(subtitle, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val linearLayout = LinearLayout(context).apply {
            this.orientation = HORIZONTAL
            layoutDirection = LAYOUT_DIRECTION_LTR
            addView(play, LayoutParams((36 * dp).toInt(), MATCH_PARENT).apply {
                setMargins((10 * dp).toInt(), 0, (12 * dp).toInt(), 0)
            })
            addView(next, LayoutParams((36 * dp).toInt(), MATCH_PARENT))
        }

        addView(textLayout, LayoutParams(0, WRAP_CONTENT, 1f))
        addView(linearLayout, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    }

    init {
        val dp = resources.displayMetrics.density
        orientation = VERTICAL
        val s = (48 * dp).toInt()
        addView(image, LayoutParams(s, s))
        addView(controls, LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
            topMargin = (8 * dp).toInt()
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
        image.setImageBitmap(data.cover)
        if (data.cover != null) {
            image.scaleType = if (data.cover.width > data.cover.height)
                ImageView.ScaleType.FIT_END
            else ImageView.ScaleType.FIT_START
        }
        with(title) {
            text = data.name
        }
        with(subtitle) {
            if (data.album != null && data.artist != null)
                text = "${data.album} • ${data.artist}"
            else {
                val t = data.album ?: data.artist
                if (t == null) isVisible = true
                else text = t
            }
        }

        updatePlayButton()
    }

    fun applyCustomizations() {
        val titleColor = ColorThemer.foreground(context)
        val textColor = ColorThemer.hint(context)
        title.setTextColor(titleColor)
        subtitle.setTextColor(textColor)
        play.imageTintList = ColorStateList.valueOf(titleColor)
        next.imageTintList = ColorStateList.valueOf(titleColor)
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