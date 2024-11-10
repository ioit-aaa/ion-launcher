package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
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
            setting(R.string.background_opacity, isVertical = true) {
                seekbar("drawer:bg:alpha", 0xdd, min = 0, max = 0xff)
            }
        }
    }
}