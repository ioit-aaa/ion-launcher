package one.zagura.IonLauncher.provider.notification

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.palette.graphics.Palette
import one.zagura.IonLauncher.data.media.MediaPlayerData
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.ui.ionApplication

object MediaItemCreator {

    fun create(context: Context, controller: MediaController, mediaMetadata: MediaMetadata): MediaPlayerData {

        val title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: null
        val subtitle = mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: run {
                val album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
                val artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                if (album != null && artist != null)
                    "$album • $artist"
                else album ?: artist
            }
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: context.packageManager.getApplicationInfo(controller.packageName, 0).loadLabel(context.packageManager)

        var cover = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: null

        var color = 0
        var textColor = 0
        if (cover != null && !Battery.PowerSaver.getResource() && context.ionApplication.settings["media:tint", false]) {
            cover = when {
                cover.width > cover.height -> Bitmap.createBitmap(cover, (cover.width - cover.height) / 2, 0, cover.height, cover.height)
                cover.width < cover.height -> Bitmap.createBitmap(cover, 0, (cover.height - cover.width) / 2, cover.width, cover.width)
                else -> cover
            }
            val h = cover.height
            val b = Bitmap.createBitmap(cover, h - h / 3, 0, h / 3, h)
            val palette = Palette.from(b).generate()
            b.recycle()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch
            if (swatch != null) {
                color = swatch.rgb
                textColor = if (ColorThemer.lightness(color) > 0.6) 0xff000000.toInt() else 0xffffffff.toInt()
            }
        }

        return MediaPlayerData(
            controller = controller,
            title = title.toString(),
            subtitle = subtitle,
            cover = cover,
            color = color,
            textColor = textColor,
        )
    }
}