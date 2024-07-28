package one.zagura.IonLauncher.provider.summary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import one.zagura.IonLauncher.data.summary.BatteryStatus
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.ui.ionApplication

object Battery : UpdatingResource<BatteryStatus>() {

    object Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            status = getStatus(context)
            update(status)
        }
    }

    private var status: BatteryStatus = BatteryStatus.Charged
    override fun getResource() = status

    fun isCharging(battery: BatteryManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            battery.isCharging
        else false
    }

    fun getStatus(context: Context) : BatteryStatus {
        val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level == 100)
            return BatteryStatus.Charged

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isCharging(battery)) {
            val remainingTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                battery.computeChargeTimeRemaining()
            else -1
            return BatteryStatus.Charging(level, remainingTime)
        }

        val remainingTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            power.batteryDischargePrediction?.toMillis() ?: -1
        else -1
        return BatteryStatus.Discharging(level, remainingTime)
    }
}