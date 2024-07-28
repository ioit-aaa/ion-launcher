package one.zagura.IonLauncher.ui

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting

class PinnedGridSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.pinned_grid) {
            setting(R.string.columns, isVertical = true) {
                seekbar("dock:columns", 5, min = 1, max = 7)
            }
            setting(R.string.rows, isVertical = true) {
                seekbar("dock:rows", 2, min = 1, max = 4)
            }
            setting(R.string.icon_size, isVertical = true) {
                seekbar("dock:icon-size", 48, min = 24, max = 72, multiplier = 8)
            }
        }
    }
}