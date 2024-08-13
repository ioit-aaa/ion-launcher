package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.colorSettings
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
            colorSettings("drawer", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
        }
    }
}