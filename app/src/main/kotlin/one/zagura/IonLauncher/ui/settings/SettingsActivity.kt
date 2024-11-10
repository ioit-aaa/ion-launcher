package one.zagura.IonLauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.items.ContactsLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.drawer.HiddenAppsActivity
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.title
import one.zagura.IonLauncher.ui.settings.widgetChooser.WidgetChooserActivity
import one.zagura.IonLauncher.util.Utils

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.tweaks) {
            setting(R.string.color) { onClick(ColorSettingsActivity::class.java) }
            setting(R.string.icons) { onClick(IconsSettingsActivity::class.java) }
            setting(R.string.cards) { onClick(CardsSettingsActivity::class.java) }
            setting(R.string.drawer) { onClick(DrawerSettingsActivity::class.java) }
            setting(R.string.icon_size, isVertical = true) {
                seekbar("dock:icon-size", 48, min = 24, max = 72, multiplier = 8)
            }
            setting(R.string.radius_percent, isVertical = true) {
                seekbar("icon:radius-ratio", 25, min = 0, max = 50, multiplier = 5)
            }
            setting(R.string.columns, isVertical = true) {
                seekbar("dock:columns", 5, min = 2, max = 7)
            }
            setting(R.string.background_opacity, isVertical = true) {
                seekbar("wall:bg:alpha", 0xdd, min = 0, max = 0x33)
            }
            title(R.string.pinned_grid)
            setting(R.string.rows, isVertical = true) {
                seekbar("dock:rows", 2, min = 1, max = 5)
            }
            title(R.string.widgets)
            setting(R.string.choose_widget) {
                onClick(WidgetChooserActivity::class.java)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val con = ContactsLoader.hasPermission(view.context)
                val cal = EventsLoader.hasPermission(view.context)
                val sug = SuggestionsManager.hasPermission(view.context)
                val not = NotificationService.hasPermission(view.context)
                if (!not || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(con && cal && sug)))
                    title(R.string.grant_permissions)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!con) setting(R.string.contacts_access, subtitle = R.string.to_show_in_search) {
                        onClick {
                            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 0)
                        }
                    }
                    if (!cal) setting(
                        R.string.calendar_access,
                        subtitle = R.string.to_show_upcoming_events
                    ) {
                        onClick {
                            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 0)
                        }
                    }
                    if (!sug) setting(R.string.usage_access, subtitle = R.string.for_suggestions) {
                        onClick {
                            it.context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
                if (!not) setting(
                    R.string.notification_access,
                    subtitle = R.string.to_show_media_player
                ) {
                    onClick {
                        it.context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            title(R.string.other)
            setting(R.string.hidden_apps) { onClick(HiddenAppsActivity::class.java) }
            setting(R.string.choose_launcher) {
                onClick {
                    Utils.chooseDefaultLauncher(it.context)
                }
            }
        }
    }
}