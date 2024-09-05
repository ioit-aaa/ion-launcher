package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Bundle
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.colorSettings
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch
import one.zagura.IonLauncher.ui.view.settings.title

class CardsSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.cards) {
            colorSettings("card", ColorThemer.DEFAULT_DARK, ColorThemer.DEFAULT_LIGHT, 0xdd)
            setting(R.string.skeuomorphism) {
                switch("card:skeumorph", false)
            }
            title(R.string.suggestions)
            if (BuildConfig.DEBUG) setting(R.string.suggestions) {
                onClick(BuildConfig.APPLICATION_ID + ".debug.suggestions.DebugSuggestionsActivity")
            }
            setting(R.string.count, isVertical = true) {
                seekbar("suggestion:count", 4, min = 0, max = 6)
            }
            setting(R.string.show_search_in_suggestions) {
                switch("layout:search-in-suggestions", true)
            }
            setting(R.string.labels) {
                switch("suggestion:labels", false)
            }
            title(R.string.media_player)
            setting(R.string.media_player) {
                switch("media:show-card", true)
            }
            setting(R.string.dynamically_tint) {
                switch("media:tint", false)
            }
        }
    }
}