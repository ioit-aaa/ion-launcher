package one.zagura.IonLauncher.data.items

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class TorchToggleItem(private val cameraId: String) : LauncherItem() {

    override fun toString() = ""

    @SuppressLint("MissingSuperCall")
    override fun open(view: View, bounds: Rect) = open(view)

    @SuppressLint("MissingSuperCall")
    override fun open(view: View) {
        val camManager = view.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager? ?: return
        camManager.setTorchMode(cameraId, false)
    }
}