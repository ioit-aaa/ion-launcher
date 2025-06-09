package one.zagura.IonLauncher.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.ColorThemer.ColorSetting
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.Gestures.ACTION_LOCK
import one.zagura.IonLauncher.ui.view.Gestures.ACTION_OPEN_MENU_POPUP
import one.zagura.IonLauncher.ui.view.Gestures.ACTION_OPEN_NOTIFICATIONS
import one.zagura.IonLauncher.ui.view.settings.gestureChooser
import one.zagura.IonLauncher.ui.view.settings.onClick
import one.zagura.IonLauncher.ui.view.settings.seekbar
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.switch
import one.zagura.IonLauncher.ui.view.settings.title

class GestureSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.gestures) {
            setting(R.string.double_tap) {
                gestureChooser("gest:2tap", ACTION_LOCK)
            }
            setting(R.string.home_button) {
                gestureChooser("gest:home", Intent.ACTION_ALL_APPS)
            }
            setting(R.string.long_press) {
                gestureChooser("gest:long-press", ACTION_OPEN_MENU_POPUP)
            }
            setting(R.string.swipe_left_to_right) {
                gestureChooser("gest:l2r", null)
            }
            setting(R.string.swipe_right_to_left) {
                gestureChooser("gest:r2l", null)
            }
            setting(R.string.swipe_down) {
                gestureChooser("gest:down", ACTION_OPEN_NOTIFICATIONS)
            }
        }
    }
}