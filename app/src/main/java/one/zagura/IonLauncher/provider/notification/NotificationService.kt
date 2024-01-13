package one.zagura.IonLauncher.provider.notification

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.UpdatingResource

class NotificationService : NotificationListenerService() {

    override fun onCreate() {
        if (!NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(applicationContext.packageName)) {
            stopSelf()
        }
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.addOnActiveSessionsChangedListener(onMediaControllersUpdated, componentName)
    }

    override fun onDestroy() {
        super.onDestroy()
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.removeOnActiveSessionsChangedListener(onMediaControllersUpdated)
    }

    private val onMediaControllersUpdated = { it: MutableList<MediaController>? ->
        MediaObserver.onMediaControllersUpdated(it)
    }

    object MediaObserver : UpdatingResource<MediaPlayerData?>() {
        override fun getResource() = mediaItem

        var mediaItem: MediaPlayerData? = null
            private set

        private fun onUpdate(data: MediaPlayerData?) {
            update(data)
        }

        fun updateMediaItem(context: Context) {
            if (!NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName))
                return
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            onMediaControllersUpdated(msm.getActiveSessions(componentName))
        }

        fun onMediaControllersUpdated(controllers: MutableList<MediaController>?) {
            val old = mediaItem
            if (controllers.isNullOrEmpty()) {
                mediaItem = null
                if (old != null)
                    onUpdate(null)
                return
            }
            val controller = pickController(controllers)
            mediaItem = controller.metadata?.let {
                MediaItemCreator.create(controller, it)
            }
            if (old != mediaItem)
                onUpdate(mediaItem)
            controller.registerCallback(object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    val old = mediaItem
                    mediaItem = metadata?.let { MediaItemCreator.create(controller, it) }
                    if (old != mediaItem)
                        onUpdate(mediaItem)
                }
            })
        }

        private fun pickController(controllers: List<MediaController>): MediaController {
            for (i in controllers.indices) {
                val mc = controllers[i]
                if (mc.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    return mc
                }
            }
            return controllers[0]
        }
    }

    companion object {
        private val componentName = ComponentName(BuildConfig.APPLICATION_ID, NotificationService::class.java.name)
    }
}