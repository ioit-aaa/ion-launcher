package one.zagura.IonLauncher.provider

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.ui.ionApplication
import java.util.ArrayList

object Dock {

    inline fun getItems(context: Context, use: (i: Int, LauncherItem?) -> Unit) {
        val settings = context.ionApplication.settings
        val c = settings["dock:columns", 5] * settings["dock:rows", 2]
        settings.getStrings("pinned") { i, s ->
            if (i >= c)
                return
            use(i, LauncherItem.decode(context, s))
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