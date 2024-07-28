package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch
import one.zagura.IonLauncher.ui.iconPackPicker.IconPackPickerActivity
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.colorSettings

class IconsSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.icons) {
            setting(R.string.icon_packs) {
                onClick(IconPackPickerActivity::class.java)
            }
            setting(R.string.grayscale_icons) {
                switch("icon:grayscale", true)
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
            colorSettings("icon", ColorThemer.DEFAULT_LIGHT, ColorThemer.DEFAULT_DARK, 0xdd)
        }
    }
}