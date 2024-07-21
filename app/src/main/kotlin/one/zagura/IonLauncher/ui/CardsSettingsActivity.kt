package one.zagura.IonLauncher.ui

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.colorSettings
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView

class CardsSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.cards) {
            colorSettings("card", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
        }
    }
}