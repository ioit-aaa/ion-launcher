package one.zagura.IonLauncher.provider.search

import android.content.Context
import android.content.Intent
import android.provider.Settings
import one.zagura.IonLauncher.data.items.ActionItem
import one.zagura.IonLauncher.data.items.ContactItem

object SettingsProvider : BasicProvider<ActionItem> {

    private var list = emptyList<ActionItem>()

    override fun updateData(context: Context) {
        if (list.isEmpty())
            list = listOfNotNull(
                action(context, Settings.ACTION_ACCESSIBILITY_SETTINGS),
                action(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS),
                action(context, Settings.ACTION_BLUETOOTH_SETTINGS),
                action(context, Settings.ACTION_DISPLAY_SETTINGS),
                action(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
                action(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
                action(context, Intent.ACTION_POWER_USAGE_SUMMARY),
                action(context, Settings.ACTION_SOUND_SETTINGS),
                action(context, Settings.ACTION_WIFI_SETTINGS),
                action(context, Settings.ACTION_WIRELESS_SETTINGS),
            )
    }

    override fun clearData() {
        list = emptyList()
    }

    private fun action(context: Context, action: String): ActionItem? {
        val item = context.packageManager.queryIntentActivities(Intent(action), 0)
            .firstOrNull() ?: return null
        return ActionItem(action, item.loadLabel(context.packageManager).toString())
    }

    override fun getBaseData() = list
    override fun extraFactor(query: String, item: ActionItem) = 0f
}