package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineDispatcher
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.search.Search
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.util.CrashActivity
import one.zagura.IonLauncher.util.Settings
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

val Context.ionApplication
    get() = applicationContext as IonLauncherApp
val Activity.ionApplication
    get() = applicationContext as IonLauncherApp

class IonLauncherApp : Application() {

    val settings = Settings("settings")

    private val workerPool: ExecutorService = ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS, SynchronousQueue(), ThreadFactory {
            Thread(it).apply {
                isDaemon = false
            }
        }
    )

    fun task(task: () -> Unit) {
        workerPool.execute(task)
    }

    override fun onCreate() {
        super.onCreate()
        CrashActivity.init(applicationContext)
        settings.init(applicationContext)
        SuggestionsManager.onCreate(applicationContext)
        setupApps()

        NotificationService.MediaObserver.updateMediaItem(this)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    object AppReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = AppLoader.reloadApps(context.ionApplication)
    }

    private fun setupApps() {
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        launcherApps.registerCallback(AppLoader.AppCallback(this))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(
                AppReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
                }
            )
        }
        AppLoader.reloadApps(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN)
            IconLoader.clearCache()
        Search.clearData()
        SuggestionsManager.saveToStorage(this)
    }
}