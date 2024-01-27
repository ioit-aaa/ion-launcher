package one.zagura.IonLauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.items.ContactsLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.settings.common.color
import one.zagura.IonLauncher.ui.settings.common.onClick
import one.zagura.IonLauncher.ui.settings.common.permissionSwitch
import one.zagura.IonLauncher.ui.settings.common.seekbar
import one.zagura.IonLauncher.ui.settings.common.setSettingsContentView
import one.zagura.IonLauncher.ui.settings.common.setting
import one.zagura.IonLauncher.ui.settings.common.switch
import one.zagura.IonLauncher.ui.settings.common.title
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import one.zagura.IonLauncher.ui.settings.suggestions.SuggestionsActivity
import one.zagura.IonLauncher.ui.settings.widgets.WidgetChooserActivity
import one.zagura.IonLauncher.util.Utils

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.tweaks) {
            title(R.string.pinned_grid)
            setting(R.string.columns, isVertical = true) {
                seekbar("dock:columns", 5, min = 1, max = 7)
            }
            setting(R.string.rows, isVertical = true) {
                seekbar("dock:rows", 2, min = 1, max = 4)
            }
            setting(R.string.icon_size, isVertical = true) {
                seekbar("dock:icon-size", 48, min = 24, max = 72, multiplier = 8)
            }
            title(R.string.look_and_feel)
            setting(R.string.background, subtitle = "") {
                color("color:bg", ColorThemer.DEFAULT_BG)
            }
            setting(R.string.foreground, subtitle = "") {
                color("color:fg", ColorThemer.DEFAULT_FG)
            }
            setting(R.string.background_opacity, isVertical = true) {
                seekbar("color:bg:alpha", 0xdd, min = 0, max = 0xff)
            }
            setting(R.string.icon_packs) {
                onClick(IconPackPickerActivity::class.java)
            }
            setting(R.string.grayscale_icons) {
                switch("icon:grayscale", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var bg: View? = null
                setting(R.string.monochrome_icons) {
                    switch("icon:monochrome", true) { _, isChecked ->
                        bg?.isVisible = isChecked
                    }
                }
                bg = setting(R.string.monochrome_bg) {
                    switch("icon:monochrome-bg", true)
                }
                bg.isVisible = ionApplication.settings["icon:monochrome", true]
            }
            title(R.string.suggestions)
            setting(R.string.count, isVertical = true) {
                seekbar("suggestion:count", 3, min = 0, max = 4)
            }
            if (BuildConfig.DEBUG) {
                setting(R.string.suggestions) {
                    onClick(SuggestionsActivity::class.java)
                }
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
            setting(R.string.choose_launcher) {
                onClick {
                    Utils.chooseDefaultLauncher(it.context)
                }
            }
        }
    }
}