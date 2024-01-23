package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import one.zagura.IonLauncher.data.Widget

@SuppressLint("ViewConstructor")
class WidgetView internal constructor(
    private val host: WidgetHost,
    context: Context,
) : AppWidgetHostView(context) {

    companion object {

        fun new(context: Context, widget: Widget): WidgetView {
            val host = WidgetHost(context, Widget.HOST_ID)
            return host.createView(context, widget.id, widget.getWidgetProviderInfo(context)) as WidgetView
        }
    }

    fun startListening() {
        try { host.startListening() } catch (_: Exception) {}
    }

    fun stopListening() {
        try { host.stopListening() } catch (_: Exception) {}
    }

    private fun resize(newWidth: Int, newHeight: Int) {
        val density = resources.displayMetrics.density
        val width = newWidth / density
        val height = newHeight / density
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateAppWidgetSize(Bundle.EMPTY, listOf(SizeF(width, height)))
            } else {
                updateAppWidgetSize(
                    null,
                    width.toInt(),
                    height.toInt(),
                    width.toInt(),
                    height.toInt()
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        resize(right - left, bottom - top)
    }

    override fun setAppWidget(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        super.setAppWidget(appWidgetId, info)
        setPadding(0, 0, 0, 0)
    }

    val widget get() = Widget(appWidgetInfo.provider.packageName, appWidgetInfo.provider.className, appWidgetId)
}