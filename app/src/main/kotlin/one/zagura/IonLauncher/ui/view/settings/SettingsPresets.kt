package one.zagura.IonLauncher.ui.view.settings

import one.zagura.IonLauncher.R

fun SettingsPageScope.colorSettings(namespace: String, defBG: Int, defFG: Int, defAlpha: Int) {
    title(R.string.color)
    setting(R.string.background, subtitle = "") {
        color("$namespace:bg", defBG)
    }
    setting(R.string.foreground, subtitle = "") {
        color("$namespace:fg", defFG)
    }
    setting(R.string.background_opacity, isVertical = true) {
        seekbar("$namespace:bg:alpha", defAlpha, min = 0, max = 0xff)
    }
}