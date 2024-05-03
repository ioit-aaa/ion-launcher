package one.zagura.IonLauncher.provider

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.ui.ionApplication

object Dock {

    fun getItems(context: Context): List<LauncherItem?> {
        val settings = context.ionApplication.settings
        val pinned = settings.getStrings("pinned") ?: return emptyList()
        val c = settings["dock:columns", 5] * settings["dock:rows", 2]
        return pinned.take(c).map {
            LauncherItem.decode(context, it)
        }
    }

    fun setItem(context: Context, i: Int, item: LauncherItem?) {
        val settings = context.ionApplication.settings
        var pinned = settings.getStrings("pinned") ?: Array(i + 1) { "" }
        if (pinned.size <= i)
            pinned += Array(i + 1 - pinned.size) { "" }
        pinned[i] = item.toString()
        settings.edit(context) {
            "pinned" set pinned
        }
    }
}