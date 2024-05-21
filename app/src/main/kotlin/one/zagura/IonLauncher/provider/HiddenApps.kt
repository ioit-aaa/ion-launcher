package one.zagura.IonLauncher.provider

import android.content.Context
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings

object HiddenApps {

    fun getItems(context: Context): List<LauncherItem> {
        val hidden = context.ionApplication.settings.getStrings("hidden") ?: return emptyList()
        return hidden.mapNotNull { LauncherItem.decode(context, it) }
    }

    fun hide(context: Context, item: LauncherItem) {
        val settings = context.ionApplication.settings
        var hidden = settings.getStrings("hidden")
        if (hidden == null)
            hidden = arrayOf(item.toString())
        else
            hidden += item.toString()
        settings.edit(context) {
            "hidden" set hidden
        }
        if (item is App)
            AppLoader.onHide(item)
    }

    fun show(context: Context, item: LauncherItem) {
        val settings = context.ionApplication.settings
        val hidden = settings.getStrings("hidden") ?: return
        val index = hidden.indexOf(item.toString())
        if (index == -1)
            return
        val new = hidden.toMutableList().apply { removeAt(index) }.toTypedArray()
        settings.edit(context) {
            "hidden" set new
        }
        if (item is App)
            AppLoader.onShow(context, item)
    }

    fun isHidden(settings: Settings, app: App): Boolean {
        val hidden = settings.getStrings("hidden") ?: return false
        return hidden.contains(app.toString())
    }
}