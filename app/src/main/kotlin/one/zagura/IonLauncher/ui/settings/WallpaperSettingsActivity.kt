package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer.ColorSetting
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch

class WallpaperSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.wallpaper) {
            colorSettings("wall", ColorSetting.Dynamic.SHADE, ColorSetting.Dynamic.LIGHT, 0x33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                setting(R.string.zoom_wallpaper) {
                    switch("wall:zoom", true)
                }
        }
    }
}