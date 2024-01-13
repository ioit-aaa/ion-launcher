package one.zagura.IonLauncher.data.media

import android.app.PendingIntent
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView

class MediaPlayerData(
    val name: String,
    val album: String?,
    val artist: String?,
    val cover: Bitmap?,
    val onTap: PendingIntent?,
)