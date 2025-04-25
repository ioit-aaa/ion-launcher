package one.zagura.IonLauncher.provider.suggestions

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.OpenAlarmsItem
import one.zagura.IonLauncher.data.items.TorchToggleItem
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object SuggestionsManager : UpdatingResource<List<LauncherItem>>() {

    private val suggestionData = Settings("suggestionData")

    private const val MAX_CONTEXT_COUNT = 8

    private lateinit var contextMap: ContextMap<LauncherItem>
    private val contextLock = ReentrantLock()

    private var suggestions: ArrayList<LauncherItem> = ArrayList()
    override fun getResource(): List<LauncherItem> = suggestions

    private var systemActions = ArrayList<LauncherItem>()

    fun onItemOpened(context: Context, item: LauncherItem) {
        TaskRunner.submit {
            val data = getCurrentContext(context)
            contextLock.withLock {
                contextMap.push(item, data, MAX_CONTEXT_COUNT)
            }
            updateSuggestions(context)
        }
    }

    fun onCreate(context: Context) {
        suggestionData.init(context)
        contextMap = loadFromStorage(context)
    }

    fun onAppsLoaded(context: Context) {
        updateSuggestions(context)
    }

    fun onAppUninstalled(packageName: String, user: UserHandle) {
        if (this.suggestions.removeAll { it is App && it.packageName == packageName && it.userHandle == user })
            update(suggestions)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun onTorchStateChanged(context: Context, enabled: Boolean, cameraId: String) {
        val i = systemActions.indexOfFirst { it is TorchToggleItem }
        if (i != -1) {
            systemActions.removeAt(i)
            suggestions.removeAt(i)
        }
        if (enabled) {
            val item = TorchToggleItem(cameraId)
            systemActions.add(0, item)
            suggestions.add(0, item)
        }
        update(suggestions)
    }

    private fun updateSuggestions(context: Context) {
        val currentData = getCurrentContext(context)

        val newSuggestions = TreeSet<Pair<LauncherItem, Float>> { a, b ->
            when {
                a.first == b.first -> 0
                a.second < b.second -> -1
                else -> 1
            }
        }
        val dockItems = ArrayList<LauncherItem?>()
        Dock.getItems(context) { _, it -> dockItems.add(it) }
        contextLock.withLock {
            val settings = context.ionApplication.settings
            for ((item, data) in contextMap.entries) {
                if (dockItems.contains(item))
                    continue
                if (HiddenApps.isHidden(settings, item))
                    continue
                val d = contextMap.calculateDistance(currentData, data)
                if (d < 0.0004f)
                    newSuggestions.add(item to d / 0.0004f)
            }
        }

        if (newSuggestions.size < 6 && hasPermission(context)) {
            @SuppressLint("InlinedApi")
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val c = Calendar.getInstance()
            c.add(Calendar.HOUR_OF_DAY, -26)

            val stats = HashMap<String, UsageStats>().apply {
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    c.timeInMillis,
                    System.currentTimeMillis()
                ).forEach {
                    this[it.packageName] = it
                }
            }
            for (app in AppLoader.getResource()) {
                if (dockItems.contains(app))
                    continue
                val stat = stats[app.packageName] ?: continue
                val lastUse = stat.lastTimeUsed
                val sinceLastUse = (System.currentTimeMillis() - lastUse).toDuration(DurationUnit.MILLISECONDS)
                val normalized = sinceLastUse.minus(sinceLastUse.inWholeDays.toDuration(DurationUnit.DAYS))
                if (normalized.inWholeMinutes > 24)
                    continue
                val d = (normalized.inWholeMinutes / 48f + 0.5f).pow(2)
                newSuggestions.add(app to d)
            }
        }
        if (EventsLoader.mightWantToSetAlarm(context))
            newSuggestions.add(OpenAlarmsItem to 0.1f)
        val s = ArrayList(systemActions)
        suggestions = newSuggestions.mapTo(s) { it.first }
        update(suggestions)
    }

    private fun getCurrentContext(context: Context): ContextItem {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val rightNow = Calendar.getInstance()

        val batteryLevel = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isPluggedIn = Battery.isCharging(batteryManager)
        val currentMinuteIn24Format = rightNow[Calendar.HOUR_OF_DAY] * 60 + rightNow[Calendar.MINUTE]
        val isHeadSetConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).isNotEmpty()
        else false
        val connManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiOn = connManager.isWifiEnabled
        val dayOfYear = rightNow[Calendar.DAY_OF_YEAR]

        return ContextItem(
            minute = currentMinuteIn24Format,
            battery = batteryLevel,
            dayOfYear = dayOfYear,
            hasHeadset = isHeadSetConnected,
            hasWifi = isWifiOn,
            isPluggedIn = isPluggedIn,
        )
    }

    private fun loadFromStorage(context: Context): ContextMap<LauncherItem> {
        return contextLock.withLock {
            val contextMap = ContextMap<LauncherItem>()
            val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
            suggestionData.getStrings("stat:open-ctx") { i, itemRepresentation ->
                val k = "stat:open-ctx:$itemRepresentation"
                val item = LauncherItem.decode(context, itemRepresentation)
                if (item == null) {
                    suggestionData.edit(context) { setStrings(k, null) }
                    return@getStrings
                }
                val items = ArrayList<ContextItem>()
                suggestionData.getInts(k) { i, data ->
                    val item = ContextItem(data)
                    if (abs(dayOfYear - item.dayOfYear) < 30)
                        items.add(item)
                }
                if (items.isNotEmpty())
                    contextMap[item] = items
            }
            contextMap
        }
    }

    fun saveToStorage(context: Context) {
        contextLock.withLock {
            suggestionData.edit(context) {
                "stat:open-ctx" set contextMap
                    .map { it.key.toString() }
                contextMap.forEach { (item, data) ->
                    setInts("stat:open-ctx:$item", IntArray(data.size) { data[it].data })
                }
            }
        }
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false
        val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            aom.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.packageName
            )
        else aom.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )

        return if (mode == AppOpsManager.MODE_DEFAULT)
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        else
            mode == AppOpsManager.MODE_ALLOWED
    }
}