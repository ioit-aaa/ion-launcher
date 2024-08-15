package one.zagura.IonLauncher.provider

import android.content.Context
import android.graphics.drawable.Drawable
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings

object EditedItems {

    fun getIcon(context: Context, item: LauncherItem): Drawable? {
        val v = context.ionApplication.settings.getString("!icon:$item") ?: return null
        if (v.isEmpty())
            return null
        return when (v[0]) {
            'P' -> {
                val a = v.substring(1).split(':', limit = 2)
                if (a.size != 2)
                    return null
                val (packageName, resourceName) = a
                val res = context.packageManager.getResourcesForApplication(packageName)
                IconPackInfo.fromResourceName(res, packageName, resourceName,
                    context.resources.displayMetrics.densityDpi)
            }
            else -> null
        }
    }

    fun getLabel(context: Context, item: LauncherItem): String? {
        return context.ionApplication.settings.getString("!label:$item")
    }

    fun setLabel(context: Context, settings: Settings, item: LauncherItem, label: String?) {
        settings.edit(context) {
            "!label:$item" set label
        }
    }

    fun setIconPackIcon(context: Context, item: LauncherItem, packageName: String, resourceName: String) {
        context.ionApplication.settings.edit(context) {
            "!icon:$item" set "P$packageName:$resourceName"
        }
        IconLoader.invalidateItem(item)
    }

    fun resetIcon(context: Context, item: LauncherItem) {
        context.ionApplication.settings.edit(context) {
            "!icon:$item" set null
        }
        IconLoader.invalidateItem(item)
    }
}