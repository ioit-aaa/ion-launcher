package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.ColorThemer.ColorSetting
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.title

class IconsSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.icons) {
            setting(R.string.icon_packs) {
                onClick(IconPackPickerActivity::class.java)
            }
            setting(R.string.reshape_legacy) {
                switch("icon:reshape-legacy", true)
            }
            setting(R.string.grayscale_icons) {
                switch("icon:grayscale", false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var bg: View? = null
                setting(R.string.monochrome_icons) {
                    switch("icon:monochrome", false) { _, isChecked ->
                        bg?.isVisible = isChecked
                    }
                }
                bg = setting(R.string.monochrome_bg) {
                    switch("icon:monochrome-bg", true)
                }
                bg.isVisible = ionApplication.settings["icon:monochrome", false]
            }
            colorSettings("icon", ColorSetting.Dynamic.LIGHT, ColorSetting.Dynamic.SHADE, 0xdd)
            title(R.string.skeuomorphism)
            setting(R.string.icon_rim) {
                switch("icon:rim", false)
            }
            var themed: View? = null
            setting(R.string.icon_gloss) {
                switch("icon:gloss", false) { _, isChecked ->
                    themed?.isVisible = isChecked
                }
            }
            themed = setting(R.string.icon_gloss_themed) {
                switch("icon:gloss-themed", false)
            }
            themed.isVisible = ionApplication.settings["icon:gloss", false]
        }
    }
}