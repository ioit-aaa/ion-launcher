package one.zagura.IonLauncher.data.summary

sealed class BatteryStatus {
    data object Charged : BatteryStatus()
    data class Charging(val level: Int) : BatteryStatus()
    data class Discharging(val level: Int) : BatteryStatus()
}