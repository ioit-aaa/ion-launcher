package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.provider.search.Search
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.util.CrashActivity
import one.zagura.IonLauncher.util.Settings

val Context.ionApplication
    get() = applicationContext as IonLauncherApp
val Activity.ionApplication
    get() = applicationContext as IonLauncherApp

class IonLauncherApp : Application() {

    val settings = Settings("settings")

    override fun onCreate() {
        super.onCreate()
        CrashActivity.init(applicationContext)
        settings.init(applicationContext)
        SuggestionsManager.onCreate(applicationContext)

        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        launcherApps.registerCallback(AppLoader.AppCallback(this))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(AppReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            })
        }
        AppLoader.reloadApps(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
                (getSystemService(Context.CAMERA_SERVICE) as CameraManager?)
                    ?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))

        registerReceiver(Battery.Receiver, IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        registerReceiver(Battery.PowerSaver.Receiver, IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) =
            SuggestionsManager.onTorchStateChanged(this@IonLauncherApp, enabled, cameraId)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    object AppReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) =
            AppLoader.reloadApps(context.ionApplication)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            IconLoader.clearCache()
            LabelLoader.clearCache()
        }
        Search.clearData()
        SuggestionsManager.saveToStorage(this)
    }
}