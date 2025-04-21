package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.ColorThemer.ColorSetting
import one.zagura.IonLauncher.ui.ionApplication
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
            setting(R.string.skeuomorphism) {
                switch("card:skeumorph", false)
            }
            if (ionApplication.settings.has("smartspacer:replace-ataglance") ||
                try { packageManager.getPackageInfo(SmartspacerConstants.SMARTSPACER_PACKAGE_NAME, 0); true } catch (_: NameNotFoundException) { false }) {
                setting(R.string.smartspacer) {
                    switch("smartspacer:replace-ataglance", false)
                }
            }
            colorSettings("card", ColorSetting.Dynamic.SHADE_LIGHTER, ColorSetting.Dynamic.LIGHTER, 0xdd)
            title(R.string.suggestions)
            if (BuildConfig.DEBUG) setting(R.string.suggestions) {
                onClick(BuildConfig.APPLICATION_ID + ".debug.suggestions.DebugSuggestionsActivity")
            }
            setting(R.string.count_0_is_none, isVertical = true) {
                seekbar("suggestion:count", 4, min = 0, max = 6)
            }
            setting(R.string.show_search_in_suggestions) {
                switch("layout:search-in-suggestions", true)
            }
            setting(R.string.labels) {
                switch("suggestion:labels", false)
            }
            setting(R.string.pill_shaped) {
                switch("suggestion:pill", false)
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