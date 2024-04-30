package one.zagura.IonLauncher.data.media

import android.app.PendingIntent
import android.graphics.Bitmap

data class MediaPlayerData(
    val title: String,
    val subtitle: String?,
    val cover: Bitmap?,
    val onTap: PendingIntent?,
)