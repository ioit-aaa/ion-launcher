package one.zagura.IonLauncher.provider.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.ui.ionApplication

class NotificationService : NotificationListenerService() {

    override fun onCreate() {
        if (!hasPermission(applicationContext)) {
            stopSelf()
            return
        }
    }

    private fun activeSessionsCallback(controllers: MutableList<MediaController>?) {
        MediaObserver.onMediaControllersUpdated(applicationContext, controllers)
    }

    override fun onListenerConnected() {
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.addOnActiveSessionsChangedListener(::activeSessionsCallback, ComponentName(BuildConfig.APPLICATION_ID, NotificationService::class.java.name))
        MediaObserver.updateMediaItem(applicationContext)
        TopNotificationProvider.update(activeNotifications, currentRanking)
    }
    override fun onListenerDisconnected() {
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.removeOnActiveSessionsChangedListener(::activeSessionsCallback)
        activeSessionsCallback(null)
        TopNotificationProvider.update(emptyArray(), currentRanking)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) =
        TopNotificationProvider.update(activeNotifications, rankingMap)

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) =
        TopNotificationProvider.update(activeNotifications, rankingMap)

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) =
        TopNotificationProvider.update(activeNotifications, rankingMap)

    override fun onNotificationChannelModified(
        pkg: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int
    ) = TopNotificationProvider.update(activeNotifications, currentRanking)

    override fun onNotificationChannelGroupModified(
        pkg: String?,
        user: UserHandle?,
        group: NotificationChannelGroup?,
        modificationType: Int
    ) = TopNotificationProvider.update(activeNotifications, currentRanking)

    object MediaObserver : UpdatingResource<Array<MediaPlayerData>>() {

        private var mediaItems = emptyArray<MediaPlayerData>()
        override fun getResource() = mediaItems

        fun updateMediaItem(context: Context) {
            if (!hasPermission(context))
                return
            if (!context.ionApplication.settings["media:show-card", true]) {
                mediaItems = emptyArray()
                return
            }
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            onMediaControllersUpdated(context, msm.getActiveSessions(ComponentName(BuildConfig.APPLICATION_ID, NotificationService::class.java.name)))
        }

        fun onMediaControllersUpdated(context: Context, controllers: MutableList<MediaController>?) {
            if (!context.ionApplication.settings["media:show-card", true]) {
                mediaItems = emptyArray()
                return
            }
            val old = mediaItems
            for (item in old)
                item.controller.unregisterCallback(item.callback)
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
                item.callback = object : MediaController.Callback() {
                    var oldItem = item
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        if (metadata == null)
                            list.remove(oldItem)
                        else {
                            val newItem = MediaItemCreator.create(context, controller, metadata)
                            newItem.callback = this
                            if (newItem == oldItem)
                                return
                            val i = list.indexOf(oldItem)
                            if (i == -1)
                                list.add(newItem)
                            else
                                list[i] = newItem
                            oldItem = newItem
                        }
                        mediaItems = list.toTypedArray()
                        update(mediaItems)
                    }
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        update(mediaItems)
                    }
                }
                list.add(item)
                controller.registerCallback(item.callback)
            }
            mediaItems = list.toTypedArray()
            if (!old.contentEquals(mediaItems))
                update(mediaItems)
        }
    }

    companion object {
        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}