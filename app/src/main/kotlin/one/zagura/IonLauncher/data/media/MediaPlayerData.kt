package one.zagura.IonLauncher.data.media

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.view.View
import one.zagura.IonLauncher.data.items.LauncherItem

data class MediaPlayerData(
    val controller: MediaController,
    val title: CharSequence,
    val subtitle: CharSequence,
    val cover: Bitmap?,
    val color: Int,
    val textColor: Int,
) {
    lateinit var callback: MediaController.Callback

    fun onTap(view: View) {
        val session = controller.sessionActivity
        val options = LauncherItem.createOpeningAnimation(view)
        if (session != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                session.send(view.context, 0, null, null, null, null,
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle())
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                session.send(view.context, 0, null, null, null, null, options.toBundle())
            else session.send(view.context, 0, null)

        }

        val intent = controller.packageName?.let {
            view.context.packageManager.getLaunchIntentForPackage(it)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } ?: return

        view.context.startActivity(intent, options.toBundle())
    }

    val isPlaying: Boolean
        get() = controller.playbackState?.state == PlaybackState.STATE_PLAYING

    fun play() = controller.transportControls.play()
    fun pause() = controller.transportControls.pause()

    fun hasNext(): Boolean = controller.queue != null

    fun next() = controller.transportControls.skipToNext()
}