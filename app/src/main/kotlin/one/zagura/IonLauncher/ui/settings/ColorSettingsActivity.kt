package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.annotation.StringRes
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.SettingsPageScope
import one.zagura.IonLauncher.ui.view.settings.color
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.title

fun SettingsPageScope.colorSettings(@StringRes title: Int, namespace: String, defBG: Int, defFG: Int, defAlpha: Int) {
    title(title)
    setting(R.string.background, subtitle = "") {
        color("$namespace:bg", defBG)
    }
    setting(R.string.foreground, subtitle = "") {
        color("$namespace:fg", defFG)
    }
}

class ColorSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.color) {
            colorSettings(R.string.drawer, "drawer", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
            colorSettings(R.string.cards, "card", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
            colorSettings(R.string.icons, "icon", ColorThemer.DEFAULT_LIGHT, ColorThemer.DEFAULT_DARK, 0xdd)
            colorSettings(R.string.wallpaper, "wall", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0x33)
        }
    }
}