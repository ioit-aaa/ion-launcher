package one.zagura.IonLauncher.data.media

import android.app.PendingIntent
import android.graphics.Bitmap

data class MediaPlayerData(
    val title: CharSequence,
    val subtitle: CharSequence,
    val cover: Bitmap?,
    val color: Int,
    val textColor: Int,
    val onTap: PendingIntent?,
    val isPlaying: (() -> Boolean)?,
    val play: () -> Unit,
    val pause: () -> Unit,
    val next: (() -> Unit)?,
)