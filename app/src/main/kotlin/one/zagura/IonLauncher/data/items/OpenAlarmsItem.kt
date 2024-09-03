package one.zagura.IonLauncher.data.items

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.provider.AlarmClock
import android.view.View

object OpenAlarmsItem : LauncherItem() {

    override fun toString() = ""

    @SuppressLint("MissingSuperCall")
    override fun open(view: View, bounds: Rect) = open(view)

    @SuppressLint("MissingSuperCall")
    override fun open(view: View) {
        view.context.startActivity(
            Intent(AlarmClock.ACTION_SHOW_ALARMS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), createOpeningAnimation(view).toBundle())
    }
}