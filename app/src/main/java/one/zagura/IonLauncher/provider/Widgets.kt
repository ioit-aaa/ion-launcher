package one.zagura.IonLauncher.provider

import android.content.Context
import one.zagura.IonLauncher.data.Widget
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.WidgetHost

object Widgets {

    fun getWidget(context: Context): Widget? {
        val settings = context.ionApplication.settings
        val widget = settings.getString("widget") ?: return null
        return Widget.decode(context, widget)
    }

    fun setWidget(context: Context, widget: Widget?) {
        val settings = context.ionApplication.settings
        val oldWidget = settings.getString("widget")?.let { Widget.decode(context, it) }
        val host = WidgetHost(context, Widget.HOST_ID)
        oldWidget?.id?.let(host::deleteAppWidgetId)
        settings.edit(context) {
            "widget" set widget?.toString()
        }
    }
}