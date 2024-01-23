package one.zagura.IonLauncher.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import one.zagura.IonLauncher.BuildConfig

data class Widget(
    val packageName: String,
    val name: String,
    val id: Int,
) {

    override fun toString() = "$packageName/$name/${id.toString(16)}"

    fun getWidgetProviderInfo(context: Context): AppWidgetProviderInfo? {
        val widgetManager = AppWidgetManager.getInstance(context)
        val appWidgets = widgetManager.installedProviders
        var providerInfo: AppWidgetProviderInfo? = null
        for (w in appWidgets) {
            if (w.provider.packageName == packageName && w.provider.className == name) {
                providerInfo = w
                break
            }
        }
        return providerInfo
    }

    companion object {
        val HOST_ID: Int = BuildConfig.APPLICATION_ID.hashCode()

        fun decode(context: Context, data: String): Widget? {
            val s = data.split("/").toTypedArray()
            if (s.size < 3)
                return null
            val packageName = s[0]
            val className = s[1]
            val id = s[2].toIntOrNull(16) ?: return null
            return Widget(packageName, className, id).takeIf { it.getWidgetProviderInfo(context) != null }
        }
    }
}