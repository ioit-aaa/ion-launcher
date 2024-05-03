package one.zagura.IonLauncher.provider.notification

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import one.zagura.IonLauncher.data.media.MediaPlayerData

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

        val coverBmp = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: null

        return MediaPlayerData(
            title = title.toString(),
            subtitle = subtitle,
            cover = coverBmp,
            onTap = controller.sessionActivity,
            isPlaying = { controller.playbackState?.state == PlaybackState.STATE_PLAYING },
            play = controller.transportControls::play,
            pause = controller.transportControls::pause,
            next = controller.transportControls::skipToNext,
        )
    }
}