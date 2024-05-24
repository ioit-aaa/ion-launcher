package one.zagura.IonLauncher.ui

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch
import one.zagura.IonLauncher.ui.view.settings.title

class LayoutSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.layout) {
            setting(R.string.radius_percent, isVertical = true) {
                seekbar("icon:radius-ratio", 50, min = 0, max = 50, multiplier = 5)
            }
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
            title(R.string.suggestions)
            if (BuildConfig.DEBUG) setting(R.string.suggestions) {
                onClick(BuildConfig.APPLICATION_ID + ".debug.suggestions.DebugSuggestionsActivity")
            }
            setting(R.string.count, isVertical = true) {
                seekbar("suggestion:count", 3, min = 0, max = 4)
            }
            setting(R.string.show_search_in_suggestions) {
                switch("layout:search-in-suggestions", false)
            }
        }
    }
}