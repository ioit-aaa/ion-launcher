package one.zagura.IonLauncher.provider.suggestions

import kotlin.math.abs
import kotlin.math.min

/**
 * minuteOfYear: 20
 * battery: 7
 * hasHeadset, hasWifi, isPluggedIn: 3
 *
 * total: 30
 */
@JvmInline
value class ContextItem(
    val data: Int
) {
    constructor(
        minute: Int,
        battery: Int,
        dayOfYear: Int,
        hasHeadset: Boolean,
        hasWifi: Boolean,
        isPluggedIn: Boolean,
    ) : this(
        (((dayOfYear * (24 * 60) + minute) and BITS_MINUTE_OF_YEAR) shl OFFSET_MINUTE_OF_YEAR) or
        ((battery and BITS_BATTERY) shl OFFSET_BATTERY) or
        ((if (hasHeadset) 1 else 0) shl OFFSET_HAS_HEADSET) or
        ((if (hasWifi) 1 else 0) shl OFFSET_HAS_WIFI) or
        ((if (isPluggedIn) 1 else 0) shl OFFSET_IS_PLUGGED_IN)
    )

    inline val minuteOfYear: Int
        get() = (data shr OFFSET_MINUTE_OF_YEAR) and OFFSET_MINUTE_OF_YEAR

    inline val battery: Int
        get() = (data shr OFFSET_BATTERY) and BITS_BATTERY

    inline val hasHeadset: Boolean
        get() = (data shr OFFSET_HAS_HEADSET) and 1 != 0

    inline val hasWifi: Boolean
        get() = (data shr OFFSET_HAS_WIFI) and 1 != 0

    inline val isPluggedIn: Boolean
        get() = (data shr OFFSET_IS_PLUGGED_IN) and 1 != 0

    inline val minuteOfDay: Int
        get() = minuteOfYear % (24 * 60)

    inline val dayOfYear: Int
        get() = minuteOfYear / (24 * 60)

    inline val weekDay: Int
        get() = dayOfYear % 7

    companion object {
        const val BITS_MINUTE_OF_YEAR = 20
        const val BITS_BATTERY = 7

        const val OFFSET_IS_PLUGGED_IN = 0
        const val OFFSET_HAS_WIFI = 1
        const val OFFSET_HAS_HEADSET = 2
        const val OFFSET_BATTERY = 3
        const val OFFSET_MINUTE_OF_YEAR = OFFSET_BATTERY + BITS_BATTERY

        private fun difMinuteOfDay(a: Int, b: Int): Float {
            val w = 1f / (24f * 60f)
            val base = abs(a - b) * w
            val dist = min(base, 1f - base)
            val inv = 1f - dist
            return 1f - (inv * inv * inv)
        }

        private fun difBattery(a: Int, b: Int): Float {
            val w = 1f / 100f / 5f // (not as important usually)
            val dist = abs(a - b) * w
            val inv = 1f - dist
            return 1f - (inv * inv * inv)
        }

        private fun difDayOfYear(a: Int, b: Int): Float {
            val w = 1f / 365f
            val dist = abs(a - b) * w
            val inv = 1f - dist
            return 1f - (inv * inv * inv)
        }

        private fun difWeekDay(a: Int, b: Int): Float {
            val w = 1f / 6f
            val dist = abs(a - b) * w
            val inv = 1f - dist
            return 1f - (inv * inv * inv)
        }

        private fun differentiator(a: Boolean, b: Boolean): Float {
            return if (a xor b) 1f else 0f
        }

        fun calculateDistance(a: ContextItem, b: ContextItem): Float {
            var sum = 0f
            sum += difMinuteOfDay(a.minuteOfDay, b.minuteOfDay)
            sum += difBattery(a.battery, b.battery)
            sum += difDayOfYear(a.dayOfYear, b.dayOfYear)
            sum += difWeekDay(a.weekDay, b.weekDay)
            sum += differentiator(a.hasHeadset, b.hasHeadset)
            sum += differentiator(a.hasWifi, b.hasWifi)
            sum += differentiator(a.isPluggedIn, b.isPluggedIn)
            return sum / 7
        }

        fun mix(a: ContextItem, b: ContextItem): ContextItem = ContextItem(
            minute = (a.minuteOfDay + b.minuteOfDay) / 2,
            battery = (a.battery + b.battery) / 2,
            dayOfYear = (a.dayOfYear + b.dayOfYear) / 2,
            hasHeadset = a.hasHeadset && b.hasHeadset,
            hasWifi = a.hasWifi && b.hasWifi,
            isPluggedIn = a.isPluggedIn && b.isPluggedIn,
        )
    }
}