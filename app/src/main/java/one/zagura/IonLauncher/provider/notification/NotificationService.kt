package one.zagura.IonLauncher.provider.notification

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
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

    object MediaObserver : UpdatingResource<Set<MediaPlayerData>>() {
        override fun getResource() = mediaItems

        private var mediaItems = emptySet<MediaPlayerData>()

        fun updateMediaItem(context: Context) {
            if (!hasPermission(context))
                return
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            onMediaControllersUpdated(msm.getActiveSessions(componentName))
        }

        fun onMediaControllersUpdated(controllers: MutableList<MediaController>?) {
            val old = mediaItems
            if (controllers.isNullOrEmpty()) {
                mediaItems = emptySet()
                if (old.isNotEmpty())
                    update(mediaItems)
                return
            }
            val set = HashSet<MediaPlayerData>()
            for (controller in controllers) {
                val metadata = controller.metadata ?: continue
                val item = MediaItemCreator.create(controller, metadata)
                set.add(item)
                controller.registerCallback(object : MediaController.Callback() {
                    var oldItem = item
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        set.remove(oldItem)
                        if (metadata != null) {
                            val newItem = MediaItemCreator.create(controller, metadata)
                            set.add(newItem)
                            if (newItem == oldItem) {
                                oldItem = newItem
                                return
                            }
                            oldItem = newItem
                        }
                        update(mediaItems)
                    }
                })
            }
            mediaItems = set
            if (old != mediaItems)
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