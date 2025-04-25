package one.zagura.IonLauncher.provider

import android.content.Context
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings

object HiddenApps {

    const val KEY = "hidden"

    inline fun getItems(context: Context, use: (LauncherItem) -> Unit) {
        context.ionApplication.settings.getStrings(KEY) { _, item ->
            LauncherItem.decode(context, item)?.let(use)
        }
    }

    fun hide(context: Context, item: LauncherItem) {
        val settings = context.ionApplication.settings
        var hidden = settings.getStrings(KEY)
        val i = item.toString()
        if (hidden == null)
            hidden = arrayOf(i)
        else if (hidden.contains(i)) return
        else hidden += i
        settings.edit(context) {
            KEY set hidden
        }
        if (item is App)
            AppLoader.onHide(item)
    }

    fun show(context: Context, item: LauncherItem) {
        val settings = context.ionApplication.settings
        val hidden = settings.getStrings(KEY) ?: return
        val index = hidden.indexOf(item.toString())
        if (index == -1)
            return
        val new = hidden.toMutableList().apply { removeAt(index) }.toTypedArray()
        settings.edit(context) {
            KEY set new
        }
        if (item is App)
            AppLoader.onShow(context, item)
    }

    fun isHidden(settings: Settings, item: LauncherItem): Boolean {
        val str = item.toString()
        settings.getStrings(KEY) { _, s ->
            if (s == str)
                return true
        }
        return false
    }

    fun reshow(context: Context, item: App) {
        val settings = context.ionApplication.settings
        if (isHidden(settings, item))
            return
        AppLoader.onHide(item)
        AppLoader.onShow(context, item)
    }
}