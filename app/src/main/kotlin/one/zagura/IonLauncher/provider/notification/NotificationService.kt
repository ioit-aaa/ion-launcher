package one.zagura.IonLauncher.provider.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.Person
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.data.summary.TopNotification
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

    override fun onListenerConnected() = Top.update(activeNotifications, currentRanking)
    override fun onListenerDisconnected() = Top.update(emptyArray(), currentRanking)

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) =
        Top.update(activeNotifications, rankingMap)

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) =
        Top.update(activeNotifications, rankingMap)

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) =
        Top.update(activeNotifications, rankingMap)

    override fun onNotificationChannelModified(
        pkg: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int
    ) = Top.update(activeNotifications, currentRanking)

    override fun onNotificationChannelGroupModified(
        pkg: String?,
        user: UserHandle?,
        group: NotificationChannelGroup?,
        modificationType: Int
    ) = Top.update(activeNotifications, currentRanking)

    object Top : UpdatingResource<TopNotification?>() {

        private var notification: TopNotification? = null
        override fun getResource() = notification

        fun update(all: Array<StatusBarNotification>, rankingMap: RankingMap) {
            var topNotification: TopNotification? = null
            var topScore = -1
            val r = Ranking()
            for (n in all) {
                if (!n.isClearable)
                    continue
                var score = 0
                if (!n.isOngoing)
                    score += 10

                val extra = n.notification.extras

                val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (!rankingMap.getRanking(n.key, r)) 0
                    else when (r.importance) {
                        NotificationManager.IMPORTANCE_DEFAULT -> 0
                        NotificationManager.IMPORTANCE_HIGH -> 40
                        else -> continue
                    }
                } else {
                    when (n.notification.priority) {
                        Notification.PRIORITY_DEFAULT -> 0
                        Notification.PRIORITY_HIGH -> 20
                        Notification.PRIORITY_MAX -> 50
                        else -> continue
                    }
                }
                score += importance

                var title: CharSequence? = null
                var subtitle: CharSequence? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val message = getLatestMessageFromBundleArray(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            extra.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
                        else
                            extra.getParcelableArray(Notification.EXTRA_MESSAGES) as Array<Parcelable>
                    ) ?: getLatestMessageFromBundleArray(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            extra.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES, Parcelable::class.java)
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            extra.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES) as Array<Parcelable>
                        else null
                    )
                    if (message != null) {
                        score += 100
                        if (message.isImportant)
                            score += 20

                        var name = message.sender

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val convTitle = extra.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                            name = if (name == null) convTitle
                            else if (convTitle == null) name
                            else "$convTitle • $name"
                        }
                        if (name != null) {
                            title = name
                            subtitle = message.text
                        }
                    }
                }

                if (title == null) {
                    val text = extra.getCharSequence(Notification.EXTRA_TEXT)
                        ?: extra.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
                    title = extra.getCharSequence(Notification.EXTRA_TITLE)
                    title = if (text == null)
                        title
                    else if (title == null)
                        text
                    else "$title: $text"
                }

                if (score <= topScore)
                    continue
                topNotification = TopNotification(
                    n.key,
                    title?.toString() ?: continue,
                    subtitle?.toString(),
                    n.notification.contentIntent,
                )
                topScore = score
            }
            notification = topNotification
            update(notification)
        }

        val KEY_TEXT = "text"
        val KEY_TIMESTAMP = "time"
        val KEY_SENDER = "sender"
        val KEY_SENDER_PERSON = "sender_person"

        private class ExtractedMessage(
            val sender: CharSequence?,
            val text: CharSequence,
            val isImportant: Boolean,
        )

        private fun getMessageSenderNameAndImportance(bundle: Bundle): Pair<CharSequence?, Boolean> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val senderPerson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    bundle.getParcelable(KEY_SENDER_PERSON, Person::class.java)
                else bundle.getParcelable(KEY_SENDER_PERSON)
                if (senderPerson != null)
                    return senderPerson.name to senderPerson.isImportant
            }
            return bundle.getCharSequence(KEY_SENDER) to false
        }

        private fun getMessageTimestamp(bundle: Bundle): Long =
            if (!bundle.containsKey(KEY_TIMESTAMP) || !bundle.containsKey(KEY_TEXT)) 0
            else bundle.getLong(KEY_TIMESTAMP)

        private fun getMessageFromBundle(bundle: Bundle): ExtractedMessage? {
            try {
                val text = bundle.getCharSequence(KEY_TEXT) ?: return null
                val (name, isImportant) = getMessageSenderNameAndImportance(bundle)
                return ExtractedMessage(name, text, isImportant)
            } catch (e: ClassCastException) { return null }
        }

        private fun getLatestMessageFromBundleArray(bundles: Array<Parcelable>?): ExtractedMessage? {
            if (bundles == null)
                return null
            var lastBundle: Bundle? = null
            var lastTimestamp = 0L
            for (bundle in bundles) {
                if (bundle !is Bundle)
                    continue
                val timestamp = getMessageTimestamp(bundle)
                if (timestamp == 0L)
                    continue
                if (lastTimestamp > timestamp)
                    continue
                lastTimestamp = timestamp
                lastBundle = bundle
            }
            return lastBundle?.let(::getMessageFromBundle)
        }
    }

    object MediaObserver : UpdatingResource<Array<MediaPlayerData>>() {

        private var mediaItems = emptyArray<MediaPlayerData>()
        override fun getResource() = mediaItems

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