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
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings
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

    private const val MAX_CONTEXT_COUNT = 6

    private lateinit var contextMap: ContextMap<LauncherItem>
    private val contextLock = ReentrantLock()

    private var suggestions: MutableList<LauncherItem> = ArrayList()
    override fun getResource(): List<LauncherItem> = suggestions

    fun onItemOpened(context: Context, item: LauncherItem) {
        context.ionApplication.task {
            contextLock.withLock {
                val data = ContextArray()
                getCurrentContext(context, data)
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

    fun onAppUninstalled(context: Context, packageName: String, user: UserHandle) {
        this.suggestions.removeAll { it is App && it.packageName == packageName && it.userHandle == user }
        update(suggestions)
    }

    private fun updateSuggestions(context: Context) {
        val timeBased = if (hasPermission(context)) {
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
            AppLoader.getResource().sortedBy {
                val stat = stats[it.packageName] ?: return@sortedBy 1f
                val lastUse = stat.lastTimeUsed
                val c = (System.currentTimeMillis() - lastUse).toDuration(DurationUnit.MILLISECONDS)
                val normalized = c.minus(c.inWholeDays.toDuration(DurationUnit.DAYS))
                (normalized.inWholeMinutes / 40f).coerceAtMost(1f).pow(2)
            }
        } else emptyList()

        val currentData = ContextArray()
        getCurrentContext(context, currentData)

        contextLock.withLock {
            val sortedEntries = contextMap.entries.sortedBy { (_, data) ->
                contextMap.calculateDistance(currentData, data)
            }
            this.suggestions = sortedEntries.mapTo(ArrayList()) { it.key }.run {
                addAll(timeBased)
                toMutableSet().toMutableList()
            }
        }
        update(this.suggestions)
    }

    private fun getCurrentContext(context: Context, out: ContextArray) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val rightNow = Calendar.getInstance()

        val batteryLevel = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isPluggedIn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            batteryManager
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        else false
        val currentHourIn24Format = rightNow[Calendar.HOUR_OF_DAY] * 60 + rightNow[Calendar.MINUTE]
        val weekDay = rightNow[Calendar.DAY_OF_WEEK]
        val isHeadSetConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).isNotEmpty()
        else false
        val connManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiOn = connManager.isWifiEnabled
        val dayOfYear = rightNow[Calendar.DAY_OF_YEAR]

        out.hour = currentHourIn24Format
        out.battery = batteryLevel
        out.hasHeadset = isHeadSetConnected
        out.hasWifi = isWifiOn
        out.isPluggedIn = isPluggedIn
        out.weekDay = weekDay
        out.dayOfYear = dayOfYear
    }

    private fun loadFromStorage(context: Context): ContextMap<LauncherItem> {
        return contextLock.withLock {
            val contextMap = ContextMap<LauncherItem>(ContextArray::differentiator)
            suggestionData.getStrings("stats:app_open_ctx")?.let {
                val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
                it.forEach { itemRepresentation ->
                    val k = "stats:app_open_ctx:$itemRepresentation"
                    val strings = suggestionData.getStrings(k) ?: return@forEach
                    if (strings.size % ContextArray.CONTEXT_DATA_SIZE != 0) {
                        suggestionData.edit(context) {
                            setStrings(k, null)
                        }
                        return@forEach
                    }
                    val openingContext = strings
                        .asSequence()
                        .map { it.toShortOrNull(16) ?: 0 }
                        .chunked(ContextArray.CONTEXT_DATA_SIZE)
                        .map(::ContextArray)
                        .filter { abs(dayOfYear - it.dayOfYear) < 30 }
                        .toList()

                    LauncherItem.decode(context, itemRepresentation)?.let { item ->
                        contextMap[item] = openingContext
                    } ?: suggestionData.edit(context) {
                        setStrings(k, null)
                    }
                }
            }
            contextMap
        }
    }

    fun saveToStorage(context: Context) {
        contextLock.withLock {
            suggestionData.edit(context) {
                "stats:app_open_ctx" set contextMap
                    .map { it.key.toString() }
                    .toTypedArray()
                contextMap.forEach { (item, data) ->
                    "stats:app_open_ctx:$item" set data
                        .flatMap { it.data.toList() }
                        .map { it.toString(16) }
                        .toTypedArray()
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