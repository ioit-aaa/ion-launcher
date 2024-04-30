package one.zagura.IonLauncher.provider.notification

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.UpdatingResource

class NotificationService : NotificationListenerService() {

    override fun onCreate() {
        if (!hasPermission(applicationContext)) {
            stopSelf()
            return
        }
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.addOnActiveSessionsChangedListener(MediaObserver::onMediaControllersUpdated, componentName)
        MediaObserver.updateMediaItem(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.removeOnActiveSessionsChangedListener(MediaObserver::onMediaControllersUpdated)
        MediaObserver.onMediaControllersUpdated(null)
    }

    object MediaObserver : UpdatingResource<MediaPlayerData?>() {
        override fun getResource() = mediaItem

        private var mediaItem: MediaPlayerData? = null

        private fun onUpdate(data: MediaPlayerData?) {
            update(data)
        }

        fun updateMediaItem(context: Context) {
            if (!hasPermission(context))
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

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}