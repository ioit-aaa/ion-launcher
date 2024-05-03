package one.zagura.IonLauncher.ui.view.settings

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas

@SuppressLint("ViewConstructor")
class WallpaperDragView(
    context: Context,
    private val wallpaper: Drawable,
) : View(context) {

    private val wallAspectRatio = wallpaper.intrinsicHeight.toFloat() / wallpaper.intrinsicWidth

    private var offset = 0
    private var initOff = 0f

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val screenAspectRatio = height.toFloat() / width
        offset = if (wallAspectRatio > screenAspectRatio)
            (height - width * wallpaper.intrinsicHeight / wallpaper.intrinsicWidth) / 2
        else
            (width - height * wallpaper.intrinsicWidth / wallpaper.intrinsicHeight) / 2
    }

    override fun onDraw(canvas: Canvas) {
        val screenAspectRatio = height.toFloat() / width
        if (wallAspectRatio > screenAspectRatio)
            wallpaper.setBounds(0, offset, width, offset + width * wallpaper.intrinsicHeight / wallpaper.intrinsicWidth)
        else
            wallpaper.setBounds(offset, 0, offset + height * wallpaper.intrinsicWidth / wallpaper.intrinsicHeight, height)
        wallpaper.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                val screenAspectRatio = height.toFloat() / width
                initOff = if (wallAspectRatio > screenAspectRatio)
                    e.rawY - offset
                else
                    e.rawX - offset
            }
            MotionEvent.ACTION_MOVE -> {
                val screenAspectRatio = height.toFloat() / width
                offset = if (wallAspectRatio > screenAspectRatio)
                    (e.rawY - initOff).toInt().coerceAtLeast(height - width * wallpaper.intrinsicHeight / wallpaper.intrinsicWidth).coerceAtMost(0)
                else
                    (e.rawX - initOff).toInt().coerceAtLeast(width - height * wallpaper.intrinsicWidth / wallpaper.intrinsicHeight).coerceAtMost(0)
                invalidate()
            }
        }
        return true
    }

    private fun generateWallpaper(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas(wallpaper::draw)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun applyWallpaper(which: Int) {
        val wm = WallpaperManager.getInstance(context)
        wm.setBitmap(generateWallpaper(), null, true, which)
    }

    fun applyWallpaper() {
        val wm = WallpaperManager.getInstance(context)
        wm.setBitmap(generateWallpaper())
    }
}