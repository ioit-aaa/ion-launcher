package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.settings.common.color
import one.zagura.IonLauncher.ui.settings.common.onClick
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
            setting(R.string.background) {
                color("color:bg", ColorThemer.DEFAULT_BG)
            }
            setting(R.string.foreground) {
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
            title(R.string.other)
            setting(R.string.choose_launcher) {
                onClick {
                    Utils.chooseDefaultLauncher(it.context)
                }
            }
        }
    }
}