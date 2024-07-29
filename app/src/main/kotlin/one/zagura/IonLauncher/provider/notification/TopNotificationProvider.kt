package one.zagura.IonLauncher.provider.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import one.zagura.IonLauncher.data.summary.TopNotification
import one.zagura.IonLauncher.provider.UpdatingResource

object TopNotificationProvider : UpdatingResource<TopNotification?>() {

    private const val KEY_TEXT = "text"
    private const val KEY_TIMESTAMP = "time"
    private const val KEY_SENDER = "sender"
    private const val KEY_SENDER_PERSON = "sender_person"

    private class ExtractedMessage(
        val sender: CharSequence?,
        val text: CharSequence,
        val isImportant: Boolean,
    )

    private var notification: TopNotification? = null
    override fun getResource() = notification

    fun update(all: Array<StatusBarNotification>, rankingMap: NotificationListenerService.RankingMap) {
        var topNotification: TopNotification? = null
        var topScore = -1
        val r = NotificationListenerService.Ranking()
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