package one.zagura.IonLauncher.data.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build

data class MediaPlayerData(
    val controller: MediaController,
    val title: CharSequence,
    val subtitle: CharSequence,
    val cover: Bitmap?,
    val color: Int,
    val textColor: Int,
) {
    lateinit var callback: MediaController.Callback

    fun onTap(context: Context) {
        val session = controller.sessionActivity
        if (session != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                session.send(context, 0, null, null, null, null, options)
            } else session.send(context, 0, null)
        }

        val intent = controller.packageName?.let {
            context.packageManager.getLaunchIntentForPackage(it)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } ?: return

        context.startActivity(intent)
    }

    val isPlaying: Boolean
        get() = controller.playbackState?.state == PlaybackState.STATE_PLAYING

    fun play() = controller.transportControls.play()
    fun pause() = controller.transportControls.pause()

    fun hasNext(): Boolean = controller.queue != null

    fun next() = controller.transportControls.skipToNext()
}