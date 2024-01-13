package one.zagura.IonLauncher.provider.notification

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.media.MediaPlayerData

object MediaItemCreator {

    fun create(controller: MediaController, mediaMetadata: MediaMetadata): MediaPlayerData {

        val title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: null
        val album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: null
        val artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: null

        val coverBmp = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: null

        return MediaPlayerData(
            name = title.toString(),
            album = album,
            artist = artist,
            cover = coverBmp,
            onTap = controller.sessionActivity,
        )
    }
}