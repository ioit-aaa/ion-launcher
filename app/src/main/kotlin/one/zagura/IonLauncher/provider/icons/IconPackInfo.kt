package one.zagura.IonLauncher.provider.icons

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.*
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.*
import kotlin.collections.HashMap

class IconPackInfo private constructor(
    private val res: Resources,
    private val iconPackPackageName: String,
    private val iconResourceNames: HashMap<String, String>,
    private val calendarPrefixes: HashMap<String, String>,
    val iconModificationInfo: IconGenInfo?,
) {
    class IconGenInfo(val res: Resources) {
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var scaleFactor = 1f
    }


    @SuppressLint("DiscouragedApi")
    private fun getDrawable(iconResource: String) = res.getIdentifier(
        iconResource,
        "drawable",
        iconPackPackageName
    ).takeIf { it != 0 }

    @SuppressLint("DiscouragedApi")
    fun getDrawableName(packageName: String, name: String): String? {
        val key = "$packageName/$name"
        return calendarPrefixes[key]
            ?.let { it + Calendar.getInstance()[Calendar.DAY_OF_MONTH] }?.takeIf { res.getIdentifier(
                it,
                "drawable",
                iconPackPackageName
            ) != 0 }
            ?: iconResourceNames[key]?.takeIf { res.getIdentifier(
                it,
                "drawable",
                iconPackPackageName
            ) != 0 }
    }

    fun getDrawable(packageName: String, name: String, density: Int): Drawable? {
        val key = "$packageName/$name"
        val drawableRes = calendarPrefixes[key]
            ?.let { it + Calendar.getInstance()[Calendar.DAY_OF_MONTH] }
            ?.let(::getDrawable)
            ?: iconResourceNames[key]?.let(::getDrawable)
            ?: return null
        return try {
            res.getDrawableForDensity(drawableRes, density, null)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    companion object {
        private fun parseEntry(packageManager: PackageManager, map: HashMap<String, String>, key: String, value: String) {
            when {
                key.startsWith(":") -> {
                    val category = when (key.substring(1).uppercase()) {
                        "BROWSER" -> Intent.CATEGORY_APP_BROWSER
                        "CALCULATOR" -> Intent.CATEGORY_APP_CALCULATOR
                        "CALENDAR" -> Intent.CATEGORY_APP_CALENDAR
//                        "CAMERA" -> ???
//                        "CLOCK" -> ???
                        "CONTACTS" -> Intent.CATEGORY_APP_CONTACTS
                        "EMAIL" -> Intent.CATEGORY_APP_EMAIL
                        "GALLERY" -> Intent.CATEGORY_APP_GALLERY
//                        "PHONE" -> ???
                        "SMS" -> Intent.CATEGORY_APP_MESSAGING

                        "APP_STORE" -> Intent.CATEGORY_APP_MARKET
                        "MAPS" -> Intent.CATEGORY_APP_MAPS
                        "MUSIC" -> Intent.CATEGORY_APP_MUSIC
                        "FILES" -> Intent.CATEGORY_APP_FILES
                        "WEATHER" -> Intent.CATEGORY_APP_WEATHER
                        "FITNESS" -> Intent.CATEGORY_APP_FITNESS
                        else -> return
                    }
                    val activities = packageManager.queryIntentActivities(
                        Intent(Intent.ACTION_MAIN).addCategory(category).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    for (a in activities) {
                        val activity = a.activityInfo
                        val k = "${activity.packageName}/${activity.name}"
                        if (!map.containsKey(k))
                            map[k] = value
                    }
                }
                key.startsWith("ComponentInfo{") ->
                    map[key.substring("ComponentInfo{".length, key.length - 1)] = value
                else -> map[key] = value
            }
        }

        private fun loadIconMod(name: String, res: Resources, iconPackPackageName: String, uniformOptions: BitmapFactory.Options): Bitmap? {
            val i = res.getIdentifier(name, "drawable", iconPackPackageName)
            if (i == 0) return null
            return BitmapFactory.decodeResource(res, i, uniformOptions)
        }

        @SuppressLint("DiscouragedApi")
        fun get(
            packageManager: PackageManager,
            iconPackPackageName: String,
        ): IconPackInfo {
            val res = packageManager.getResourcesForApplication(iconPackPackageName)

            var iconModificationInfo: IconGenInfo? = null
            val iconResourceNames = HashMap<String, String>()
            val calendarPrefixes = HashMap<String, String>()

            val uniformOptions = BitmapFactory.Options().apply {
                inScaled = false
            }
            try {
                val n = res.getIdentifier("appfilter", "xml", iconPackPackageName)
                val x = if (n != 0)
                    res.getXml(n)
                else {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isValidating = false
                    val xpp = factory.newPullParser()
                    val raw = res.assets.open("appfilter.xml")
                    xpp.setInput(raw, null)
                    xpp
                }
                while (x.eventType != XmlResourceParser.END_DOCUMENT) {
                    if (x.eventType == 2) {
                        try {
                            when (x.name) {
                                "scale" -> {
                                    if (iconModificationInfo == null)
                                        iconModificationInfo = IconGenInfo(res)
                                    iconModificationInfo.scaleFactor = x.getAttributeValue(0).toFloat()
                                }
                                "item" -> {
                                    val key = x.getAttributeValue(null, "component")
                                    val value = x.getAttributeValue(null, "drawable")
                                    if (key != null && value != null)
                                        parseEntry(packageManager, iconResourceNames, key, value)
                                }
                                "calendar" -> {
                                    val key = x.getAttributeValue(null, "component")
                                    val value = x.getAttributeValue(null, "prefix")
                                    if (key != null && value != null)
                                        parseEntry(packageManager, calendarPrefixes, key, value)
                                }
                                "iconback" -> {
                                    if (iconModificationInfo == null)
                                        iconModificationInfo = IconGenInfo(res)
                                    iconModificationInfo.back = loadIconMod(
                                        x.getAttributeValue(0),
                                        res,
                                        iconPackPackageName,
                                        uniformOptions
                                    )
                                }
                                "iconmask" -> {
                                    if (iconModificationInfo == null)
                                        iconModificationInfo = IconGenInfo(res)
                                    iconModificationInfo.mask = loadIconMod(
                                        x.getAttributeValue(0),
                                        res,
                                        iconPackPackageName,
                                        uniformOptions
                                    )
                                }
                                "iconupon" -> {
                                    if (iconModificationInfo == null)
                                        iconModificationInfo = IconGenInfo(res)
                                    iconModificationInfo.front = loadIconMod(
                                        x.getAttributeValue(0),
                                        res,
                                        iconPackPackageName,
                                        uniformOptions
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    x.next()
                }
            } catch (e: Exception) { e.printStackTrace() }
            return IconPackInfo(res, iconPackPackageName, iconResourceNames, calendarPrefixes, iconModificationInfo)
        }

        @SuppressLint("DiscouragedApi")
        fun getResourceNames(res: Resources, packageName: String): List<IconPackResourceItem> {
            val strings = ArrayList<IconPackResourceItem>()
            try {
                val n = res.getIdentifier("drawable", "xml", packageName)
                val xpp = if (n != 0) res.getXml(n) else {
                    XmlPullParserFactory.newInstance().apply {
                        isValidating = false
                    }.newPullParser().apply {
                        setInput(res.assets.open("drawable.xml"), null)
                    }
                }
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    try {
                        if (xpp.eventType == 2) when (xpp.name) {
                            "item" -> strings.add(IconPackResourceItem.Icon(xpp.getAttributeValue(0)))
                            "category" -> strings.add(IconPackResourceItem.Title(xpp.getAttributeValue(0)))
                        }
                    } catch (_: Exception) {}
                    xpp.next()
                }
            } catch (_: Exception) {}
            return strings
        }

        fun fromResourceName(res: Resources, packageName: String, resourceName: String, density: Int): Drawable? {
            val id = res.getIdentifier(resourceName, "drawable", packageName)
            if (id == 0) return null
            return res.getDrawableForDensity(id, density, null)
        }

        private const val ICON_PACK_CATEGORY = "com.anddoes.launcher.THEME"

        fun getAvailableIconPacks(packageManager: PackageManager): MutableList<ResolveInfo> {
            return packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(ICON_PACK_CATEGORY),
                0
            )
        }
    }

    sealed class IconPackResourceItem(val string: String) {
        class Icon(string: String) : IconPackResourceItem(string)
        class Title(string: String) : IconPackResourceItem(string)
    }
}