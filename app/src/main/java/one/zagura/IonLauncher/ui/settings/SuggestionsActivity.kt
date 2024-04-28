package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.settings.common.color
import one.zagura.IonLauncher.ui.settings.common.onClick
import one.zagura.IonLauncher.ui.settings.common.seekbar
import one.zagura.IonLauncher.ui.settings.common.setSettingsContentView
import one.zagura.IonLauncher.ui.settings.common.setting
import one.zagura.IonLauncher.ui.settings.common.switch
import one.zagura.IonLauncher.ui.settings.common.title
import one.zagura.IonLauncher.ui.settings.suggestions.SuggestionsActivity

class SuggestionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.suggestions) {
            if (BuildConfig.DEBUG) {
                setting(R.string.suggestions) {
                    onClick(SuggestionsActivity::class.java)
                }
            }
            setting(R.string.count, isVertical = true) {
                seekbar("suggestion:count", 3, min = 0, max = 4)
            }
            setting(R.string.show_search_in_suggestions) {
                switch("layout:search-in-suggestions", false)
            }
            title(R.string.color)
            setting(R.string.background, subtitle = "") {
                color("pill:bg", ColorThemer.DEFAULT_FG)
            }
            setting(R.string.foreground, subtitle = "") {
                color("pill:fg", ColorThemer.DEFAULT_BG)
            }
            setting(R.string.background_opacity, isVertical = true) {
                seekbar("pill:bg:alpha", 0xff, min = 0, max = 0xff)
            }
        }
    }
}