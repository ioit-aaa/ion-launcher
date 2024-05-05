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
        msm.addOnActiveSessionsChangedListener(::activeSessionsCallback, componentName)
        MediaObserver.updateMediaItem(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.removeOnActiveSessionsChangedListener(::activeSessionsCallback)
        activeSessionsCallback(null)
    }

    private fun activeSessionsCallback(controllers: MutableList<MediaController>?) {
        MediaObserver.onMediaControllersUpdated(applicationContext, controllers)
    }

    object MediaObserver : UpdatingResource<Array<MediaPlayerData>>() {
        override fun getResource() = mediaItems

        private var mediaItems = emptyArray<MediaPlayerData>()

        fun updateMediaItem(context: Context) {
            if (!hasPermission(context))
                return
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            onMediaControllersUpdated(context, msm.getActiveSessions(componentName))
        }

        fun onMediaControllersUpdated(context: Context, controllers: MutableList<MediaController>?) {
            val old = mediaItems
            if (controllers.isNullOrEmpty()) {
                mediaItems = emptyArray()
                if (old.isNotEmpty())
                    update(mediaItems)
                return
            }
            val list = ArrayList<MediaPlayerData>()
            for (controller in controllers) {
                val metadata = controller.metadata ?: continue
                val item = MediaItemCreator.create(context, controller, metadata)
                list.add(item)
                controller.registerCallback(object : MediaController.Callback() {
                    var oldItem = item
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        if (metadata != null) {
                            val newItem = MediaItemCreator.create(context, controller, metadata)
                            if (newItem == oldItem)
                                return
                            list.remove(oldItem)
                            list.add(newItem)
                            oldItem = newItem
                        } else
                            list.remove(oldItem)
                        mediaItems = list.toTypedArray()
                        update(mediaItems)
                    }
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        update(mediaItems)
                    }
                })
            }
            mediaItems = list.toTypedArray()
            if (!old.contentEquals(mediaItems))
                update(mediaItems)
        }
    }

    companion object {
        private val componentName = ComponentName(BuildConfig.APPLICATION_ID, NotificationService::class.java.name)

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}