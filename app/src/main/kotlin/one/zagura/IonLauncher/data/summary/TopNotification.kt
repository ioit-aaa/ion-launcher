package one.zagura.IonLauncher.data.summary

import android.app.PendingIntent

data class TopNotification(
    val id: String,
    val title: String,
    val subtitle: String?,
    val open: PendingIntent?,
)