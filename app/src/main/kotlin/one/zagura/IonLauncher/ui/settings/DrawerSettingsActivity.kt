package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.colorSettings
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView

class DrawerSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.drawer) {
            colorSettings("drawer", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
        }
    }
}