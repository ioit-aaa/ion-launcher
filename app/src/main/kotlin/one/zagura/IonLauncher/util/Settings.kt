package one.zagura.IonLauncher.util

import android.content.Context
import one.zagura.IonLauncher.provider.ColorThemer
import org.json.JSONArray
import org.json.JSONObject
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Settings(
    private val saveFile: String
) {
    private val fileLock = ReentrantLock()
    var updated = false

    sealed class Single <V> (
        val value: V
    ) {
        abstract fun toInt(): Int
        abstract fun toBool(): Boolean
        override fun toString() = value.toString()
    }

    private class SingleInt(value: Int) : Single<Int>(value) {
        override fun toInt(): Int = value
        override fun toBool(): Boolean = value != 0
    }

    private class SingleBool(value: Boolean) : Single<Boolean>(value) {
        override fun toInt(): Int = if (value) 1 else 0
        override fun toBool(): Boolean = value
    }

    private class SingleString(value: String) : Single<String>(value) {
        override fun toInt(): Int = value.toIntOrNull() ?: 0
        override fun toBool(): Boolean = value.toBoolean()
        override fun toString() = value
    }

    private val singles: HashMap<String, Single<*>> = HashMap()

    val lists: HashMap<String, Array<Single<*>>> = HashMap()

    private var isInitialized: Boolean = false

    private val editor = SettingsEditor(this)

    class SettingsEditor(val settings: Settings) {

        operator fun set(key: String, value: Int) {
            settings.singles[key] = SingleInt(value)
        }

        operator fun set(key: String, value: Boolean) {
            settings.singles[key] = SingleBool(value)
        }

        operator fun set(key: String, value: String?) {
            if (value == null) settings.singles.keys.remove(key)
            else settings.singles[key] = SingleString(value)
        }

        operator fun set(key: String, value: Array<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = Array(value.size) { SingleString(value[it]) }
        }

        operator fun set(key: String, value: List<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = Array(value.size) { SingleString(value[it]) }
        }

        operator fun set(key: String, value: ColorThemer.ColorSetting?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.singles[key] = when (value) {
                ColorThemer.ColorSetting.Dynamic.SHADE -> SingleString("shade")
                ColorThemer.ColorSetting.Dynamic.SHADE_LIGHTER -> SingleString("shade-light")
                ColorThemer.ColorSetting.Dynamic.VIBRANT_LIGHT -> SingleString("vib-light")
                ColorThemer.ColorSetting.Dynamic.VIBRANT_DARK -> SingleString("vib-dark")
                ColorThemer.ColorSetting.Dynamic.LIGHT -> SingleString("light")
                ColorThemer.ColorSetting.Dynamic.DARK -> SingleString("dark")
                ColorThemer.ColorSetting.Dynamic.LIGHTER -> SingleString("lighter")
                ColorThemer.ColorSetting.Dynamic.DARKER -> SingleString("darker")
                is ColorThemer.ColorSetting.Static -> SingleInt(value.value)
            }
        }

        fun setStrings(key: String, value: Array<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = Array(value.size) { SingleString(value[it]) }
        }

        fun setInts(key: String, value: IntArray?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = Array(value.size) { SingleInt(value[it]) }
        }

        fun setInts(key: String, value: List<Int>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = Array(value.size) { SingleInt(value[it]) }
        }

        @JvmName("set1")
        infix fun String.set(n: Nothing?) {
            settings.singles.keys.remove(this)
        }

        @JvmName("set1")
        inline infix fun String.set(value: Int) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: ColorThemer.ColorSetting) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Boolean) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: String?) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Array<String>?) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: List<String>?) = set(this, value)
    }

    fun edit(context: Context, block: SettingsEditor.() -> Unit) {
        TaskRunner.submit {
            fileLock.withLock {
                updated = true
                block(editor)
                PrivateStorage.write(context, saveFile, ::serializeData)
            }
        }
    }

    inline fun consumeUpdate(update: () -> Unit) {
        if (updated) {
            updated = false
            update()
        }
    }

    fun saveNow(context: Context) = fileLock.withLock {
        PrivateStorage.write(context, saveFile, ::serializeData)
    }

    inline operator fun get(key: String, default: ColorThemer.ColorSetting): ColorThemer.ColorSetting = getColor(key) ?: default
    inline operator fun get(key: String, default: Int): Int = getInt(key) ?: default
    inline operator fun get(key: String, default: Boolean): Boolean = getBoolean(key) ?: default
    inline operator fun get(key: String, default: String): String = getString(key) ?: default

    inline fun getIntOr(key: String, default: () -> Int): Int = getInt(key) ?: default()
    inline fun getBoolOr(key: String, default: () -> Boolean): Boolean = getBoolean(key) ?: default()
    inline fun getStringOr(key: String, default: () -> String): String = getString(key) ?: default()

    fun getColor(key: String): ColorThemer.ColorSetting? = singles[key]?.let {
        when (it.value) {
            "shade" -> ColorThemer.ColorSetting.Dynamic.SHADE
            "shade-light" -> ColorThemer.ColorSetting.Dynamic.SHADE_LIGHTER
            "vib-light" -> ColorThemer.ColorSetting.Dynamic.VIBRANT_LIGHT
            "vib-dark" -> ColorThemer.ColorSetting.Dynamic.VIBRANT_DARK
            "light" -> ColorThemer.ColorSetting.Dynamic.LIGHT
            "dark" -> ColorThemer.ColorSetting.Dynamic.DARK
            "lighter" -> ColorThemer.ColorSetting.Dynamic.LIGHTER
            "darker" -> ColorThemer.ColorSetting.Dynamic.DARKER
            else -> ColorThemer.ColorSetting.Static(it.toInt())
        }
    }
    fun getInt(key: String): Int? = singles[key]?.toInt()
    fun getBoolean(key: String): Boolean? = singles[key]?.toBool()
    fun getString(key: String): String? = singles[key]?.toString()
    fun getStrings(key: String): Array<String>? = lists[key]?.let { l -> Array(l.size) { l[it].toString() } }
    fun getInts(key: String): IntArray? = lists[key]?.let { l -> IntArray(l.size) { l[it].toInt() } }

    inline fun getStrings(key: String, use: (i: Int, String) -> Unit): Boolean = lists[key]?.forEachIndexed { i, s -> use(i, s.toString()) } != null
    inline fun getInts(key: String, use: (i: Int, Int) -> Unit): Boolean = lists[key]?.forEachIndexed { i, s -> use(i, s.toInt()) } != null

    fun has(s: String): Boolean = singles.contains(s) || lists.contains(s)

    fun init(context: Context) {
        fileLock.withLock {
            if (!isInitialized) {
                PrivateStorage.read(context, saveFile, ::initializeData)
                isInitialized = true
            }
        }
    }

    private fun parseString(string: String): Single<*> = string
        .toBooleanStrictOrNull()?.let(::SingleBool)
        ?: string.toIntOrNull()?.let(::SingleInt)
        ?: string.let(::SingleString)

    private fun initializeData(it: ObjectInputStream): Boolean {
        val root = JSONObject(it.readUTF())
        return fill(singles, root.getJSONArray("singles")) {
            val string = getString(it)
            parseString(string)
        } or
        fill(lists, root.getJSONArray("list")) {
            val json = getJSONArray(it)
            Array(json.length()) {
                parseString(json.getString(it))
            }
        }
    }

    private fun serializeData(out: ObjectOutputStream) {
        val json = JSONObject()
        json.put("singles", toJson(singles, Single<*>::toString))
        json.put("list", toJson(lists) {
            JSONArray().apply { it.forEach(::put) }
        })
        out.writeUTF(json.toString())
    }

    private fun <T> toJson(
        map: HashMap<String, T>,
        mapper: (T) -> Any? = { it }
    ): JSONArray {
        return fileLock.withLock {
            JSONArray().apply {
                map.forEach { entry ->
                    put(JSONArray().apply {
                        put(entry.key)
                        put(mapper(entry.value))
                    })
                }
            }
        }
    }

    private fun <T> fill(
        map: HashMap<String, T>,
        entries: JSONArray,
        mapper: JSONArray.(Int) -> T
    ): Boolean {
        var hasUpdated = false
        var i = 0
        while (i < entries.length()) {
            val entry = entries.getJSONArray(i)
            val key = entry.getString(0)
            val value = mapper(entry, 1)
            if (map[key] != value) hasUpdated = true
            map[key] = value
            i++
        }
        return hasUpdated
    }
}