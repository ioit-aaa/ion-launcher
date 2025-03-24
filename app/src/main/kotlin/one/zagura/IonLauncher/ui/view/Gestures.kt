package one.zagura.IonLauncher.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Utils

object Gestures {

    const val ACTION_OPEN_MENU_POPUP = "${BuildConfig.APPLICATION_ID}.OPEN_MENU_POPUP"
    const val ACTION_OPEN_DRAWER = "${BuildConfig.APPLICATION_ID}.OPEN_DRAWER"
    const val ACTION_OPEN_NOTIFICATIONS = "${BuildConfig.APPLICATION_ID}.OPEN_NOTIFICATIONS"
    const val ACTION_OPEN_QS = "${BuildConfig.APPLICATION_ID}.OPEN_QS"
    const val ACTION_LOCK = "${BuildConfig.APPLICATION_ID}.LOCK"

    fun executeAction(context: Context, key: String, default: String?): Boolean {
        val action = (context.ionApplication.settings.getString(key) ?: default)?.ifEmpty { null } ?: return false
        when (action) {
            ACTION_LOCK -> Utils.lock(context)
            ACTION_OPEN_NOTIFICATIONS -> Utils.expandNotificationPanel(context)
            ACTION_OPEN_QS -> Utils.expandQuickSettingsPanel(context)
            else -> try {
                context.startActivity(
                    Intent(action)
                        .setPackage(BuildConfig.APPLICATION_ID)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NO_ANIMATION))
            } catch (_: Exception) {}
        }
        return true
    }

    fun onDoubleTap(context: Context) = executeAction(context, "gest:2tap", ACTION_LOCK)

    // special case to not resend intent
    fun onHomePress(context: Context): String? =
        when (val it = (context.ionApplication.settings.getString("gest:home") ?: Intent.ACTION_ALL_APPS).ifEmpty { null }) {
            ACTION_LOCK -> Utils.lock(context).let { null }
            ACTION_OPEN_NOTIFICATIONS -> Utils.expandNotificationPanel(context).let { null }
            ACTION_OPEN_QS -> Utils.expandQuickSettingsPanel(context).let { null }
            else -> it
        }

    fun onLongPress(view: View, x: Int, y: Int) {
        if (view.context.ionApplication.settings.getString("gest:long-press").let { it == ACTION_OPEN_MENU_POPUP || it.isNullOrEmpty() }) {
            val w = view.resources.displayMetrics.widthPixels
            val h = Utils.getDisplayHeight(view.context as Activity)
            LongPressMenu.popupLauncher(view, Gravity.CENTER, x - w / 2, y - h / 2)
            Utils.click(view.context)
        }
        else if (executeAction(view.context, "gest:long-press", ACTION_OPEN_MENU_POPUP))
            Utils.click(view.context)
    }

    fun onFlingLtR(context: Context) = executeAction(context, "gest:l2r", null)
    fun onFlingRtL(context: Context) = executeAction(context, "gest:r2l", null)
    fun onFlingDown(context: Context): Boolean = executeAction(context, "gest:down", ACTION_OPEN_NOTIFICATIONS)
}