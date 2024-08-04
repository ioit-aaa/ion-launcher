package one.zagura.IonLauncher.util.iconify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.RequiresApi

@SuppressLint("ViewConstructor")
@RequiresApi(Build.VERSION_CODES.Q)
class FloatingIconView(
    context: Context,
    val gnc: GestureNavContract,
    val bounds: RectF,
    val picture: Picture,
    val onEndAnim: () -> Unit
) : SurfaceView(context) {

    fun removeSelf() {
        onEndAnim()
        (parent as? ViewGroup)?.removeView(this)
    }

    init {
        setZOrderOnTop(true)
        with(holder) {
            setFormat(PixelFormat.TRANSLUCENT)
            addCallback(Callback())
        }
    }

    private inner class Callback : SurfaceHolder.Callback2 {
        override fun surfaceCreated(holder: SurfaceHolder) {
            draw(holder)
            gnc.sendEndPosition(bounds, this@FloatingIconView, surfaceControl)
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) = draw(holder)

        override fun surfaceRedrawNeeded(holder: SurfaceHolder) = draw(holder)

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        fun draw(holder: SurfaceHolder) {
            val c = holder.lockHardwareCanvas() ?: return
            picture.draw(c)
            holder.unlockCanvasAndPost(c)
        }
    }
}