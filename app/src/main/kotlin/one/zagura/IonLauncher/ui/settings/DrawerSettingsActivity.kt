package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.ColorThemer.ColorSetting
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch

class DrawerSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.drawer) {
            setting(R.string.labels) {
                switch("drawer:labels", true)
            }
            setting(R.string.open_keyboard_auto) {
                switch("drawer:auto_keyboard", false)
            }
            setting(R.string.categories) {
                switch("drawer:categories", true)
            }
            colorSettings("drawer", ColorSetting.Dynamic.SHADE, ColorSetting.Dynamic.LIGHT, 0xff)
        }
    }
}