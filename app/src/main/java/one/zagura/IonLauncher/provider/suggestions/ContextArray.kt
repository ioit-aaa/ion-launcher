package one.zagura.IonLauncher.provider.suggestions

import kotlin.math.abs
import kotlin.math.min

@JvmInline
value class ContextArray private constructor(
    val data: ShortArray
) {

    constructor() : this(ShortArray(CONTEXT_DATA_SIZE))
    constructor(list: List<Short>) : this(list.toShortArray())

    inline var hour: Int
        get() = data[CONTEXT_DATA_HOUR_OF_DAY].toInt()
        set(value) = data.set(CONTEXT_DATA_HOUR_OF_DAY, value.toShort())

    inline var battery: Int
        get() = data[CONTEXT_DATA_BATTERY].toInt()
        set(value) = data.set(CONTEXT_DATA_BATTERY, value.toShort())

    inline var hasHeadset: Boolean
        get() = data[CONTEXT_DATA_HAS_HEADSET] != 0.toShort()
        set(value) = data.set(CONTEXT_DATA_HAS_HEADSET, if (value) 1 else 0)

    inline var hasWifi: Boolean
        get() = data[CONTEXT_DATA_HAS_WIFI] != 0.toShort()
        set(value) = data.set(CONTEXT_DATA_HAS_WIFI, if (value) 1 else 0)

    inline var isPluggedIn: Boolean
        get() = data[CONTEXT_DATA_IS_PLUGGED_IN] != 0.toShort()
        set(value) = data.set(CONTEXT_DATA_IS_PLUGGED_IN, if (value) 1 else 0)

    inline var weekDay: Int
        get() = data[CONTEXT_DATA_IS_WEEK_DAY].toInt()
        set(value) = data.set(CONTEXT_DATA_IS_WEEK_DAY, value.toShort())

    inline var dayOfYear: Int
        get() = data[CONTEXT_DATA_DAY_OF_YEAR].toInt()
        set(value) = data.set(CONTEXT_DATA_DAY_OF_YEAR, value.toShort())

    companion object {
        const val CONTEXT_DATA_HOUR_OF_DAY = 0
        const val CONTEXT_DATA_BATTERY = 1
        const val CONTEXT_DATA_HAS_HEADSET = 2
        const val CONTEXT_DATA_HAS_WIFI = 3
        const val CONTEXT_DATA_IS_PLUGGED_IN = 4
        const val CONTEXT_DATA_IS_WEEK_DAY = 5
        const val CONTEXT_DATA_DAY_OF_YEAR = 6

        const val CONTEXT_DATA_SIZE = 7

        fun differentiator(i: Int, a: Short, b: Short): Float {
            val w = when (i) {
                CONTEXT_DATA_HOUR_OF_DAY -> 1f / (24 * 60)
                CONTEXT_DATA_BATTERY -> 1f / 100f
                CONTEXT_DATA_DAY_OF_YEAR -> 1f / 365f
                else -> 1f
            }
            val base = abs(a * w - b * w)
            val dist = when (i) {
                CONTEXT_DATA_HOUR_OF_DAY -> min(base, 1f - base)
                else -> base
            }
            val inv = 1 - dist
            return 1 - (inv * inv)
        }
    }
}