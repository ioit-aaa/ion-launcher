package one.zagura.IonLauncher.util

import android.content.Context
import one.zagura.IonLauncher.ui.ionApplication
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

    private abstract class Single <V> (
        val value: V
    ) {
        abstract fun toFloat(): Float
        abstract fun toInt(): Int
        abstract fun toBool(): Boolean
        override fun toString() = value.toString()
    }

    private class SingleInt(value: Int) : Single<Int>(value) {
        override fun toFloat(): Float = value.toFloat()
        override fun toInt(): Int = value
        override fun toBool(): Boolean = value != 0
    }

    private class SingleFloat(value: Float) : Single<Float>(value) {
        override fun toFloat(): Float = value
        override fun toInt(): Int = value.toInt()
        override fun toBool(): Boolean = value != 0f
    }

    private class SingleBool(value: Boolean) : Single<Boolean>(value) {
        override fun toFloat(): Float = if (value) 1f else 0f
        override fun toInt(): Int = if (value) 1 else 0
        override fun toBool(): Boolean = value
    }

    private class SingleString(value: String) : Single<String>(value) {
        override fun toFloat(): Float = value.toFloat()
        override fun toInt(): Int = value.toInt()
        override fun toBool(): Boolean = value.toBoolean()
        override fun toString() = value
    }

    private val singles: HashMap<String, Single<*>> = HashMap()

    private val lists: HashMap<String, Array<Single<*>>> = HashMap()

    private var isInitialized: Boolean = false

    private val editor = SettingsEditor(this)

    class SettingsEditor(val settings: Settings) {

        operator fun set(key: String, value: Int) {
            settings.singles[key] = SingleInt(value)
        }

        operator fun set(key: String, value: Float) {
            settings.singles[key] = SingleFloat(value)
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
            else settings.lists[key] = value.map(::SingleString).toTypedArray()
        }

        fun setStrings(key: String, value: Array<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = value.map(::SingleString).toTypedArray()
        }

        fun setInts(key: String, value: IntArray?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = value.map(::SingleInt).toTypedArray()
        }

        @JvmName("set1")
        inline infix fun String.set(value: Int) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Float) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Boolean) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: String?) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Array<String>?) = set(this, value)
    }

    fun edit(context: Context, block: SettingsEditor.() -> Unit) {
        context.ionApplication.task {
            fileLock.withLock {
                updated = true
                block(editor)
                PrivateStorage.write(context, saveFile, ::serializeData)
            }
        }
    }

    inline fun consumeUpdate(update: () -> Unit) {
        if (updated) {
            update()
            updated = false
        }
    }

    fun saveNow(context: Context) = fileLock.withLock {
        PrivateStorage.write(context, saveFile, ::serializeData)
    }

    inline operator fun get(key: String, default: Int): Int = getInt(key) ?: default
    inline operator fun get(key: String, default: Float): Float = getFloat(key) ?: default
    inline operator fun get(key: String, default: Boolean): Boolean = getBoolean(key) ?: default
    inline operator fun get(key: String, default: String): String = getString(key) ?: default

    inline fun getIntOr(key: String, default: () -> Int): Int = getInt(key) ?: default()
    inline fun getFloatOr(key: String, default: () -> Float): Float = getFloat(key) ?: default()
    inline fun getBoolOr(key: String, default: () -> Boolean): Boolean = getBoolean(key) ?: default()
    inline fun getStringOr(key: String, default: () -> String): String = getString(key) ?: default()

    fun getInt(key: String): Int? = singles[key]?.toInt()
    fun getFloat(key: String): Float? = singles[key]?.toFloat()
    fun getBoolean(key: String): Boolean? = singles[key]?.toBool()
    fun getString(key: String): String? = singles[key]?.toString()
    fun getStrings(key: String): Array<String>? = lists[key]?.map { it.toString() }?.toTypedArray()
    fun getInts(key: String): IntArray? = lists[key]?.map { it.toInt() }?.toIntArray()

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
        ?: string.toFloatOrNull()?.let(::SingleFloat)
        ?: string.let(::SingleString)

    private fun initializeData(it: ObjectInputStream): Boolean {
        val root = JSONObject(it.readUTF())
        return fill(singles, root.getJSONArray("singles")) {
            val string = getString(it)
            parseString(string)
        } or
        fill(lists, root.getJSONArray("list")) {
            val json = getJSONArray(it)
            val list = ArrayList<String>()
            var i = 0
            while (i < json.length()) {
                list.add(json.getString(i))
                i++
            }
            list.map(::parseString).toTypedArray()
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