package one.zagura.IonLauncher.util

import android.app.WallpaperManager
import android.content.Context
import android.os.IBinder

object LiveWallpaper {
    fun tap(context: Context, windowToken: IBinder, x: Int, y: Int) {
        WallpaperManager.getInstance(context).sendWallpaperCommand(
            windowToken,
            WallpaperManager.COMMAND_TAP,
            x, y, 0, null)
    }

    fun drop(context: Context, windowToken: IBinder, x: Int, y: Int) {
        WallpaperManager.getInstance(context).sendWallpaperCommand(
            windowToken,
            WallpaperManager.COMMAND_DROP,
            x, y, 0, null)
    }
}