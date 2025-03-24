package one.zagura.IonLauncher.util

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class Admin : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent) = null
}