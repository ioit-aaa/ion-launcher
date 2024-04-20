package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.settings.common.color
import one.zagura.IonLauncher.ui.settings.common.onClick
import one.zagura.IonLauncher.ui.settings.common.seekbar
import one.zagura.IonLauncher.ui.settings.common.setSettingsContentView
import one.zagura.IonLauncher.ui.settings.common.setting
import one.zagura.IonLauncher.ui.settings.common.switch
import one.zagura.IonLauncher.ui.settings.common.title
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity

class LookAndFeelActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.look_and_feel) {
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
            title(R.string.color)
            setting(R.string.background, subtitle = "") {
                color("color:bg", ColorThemer.DEFAULT_BG)
            }
            setting(R.string.foreground, subtitle = "") {
                color("color:fg", ColorThemer.DEFAULT_FG)
            }
            setting(R.string.background_opacity, isVertical = true) {
                seekbar("color:bg:alpha", 0xdd, min = 0, max = 0xff)
            }
        }
    }
}