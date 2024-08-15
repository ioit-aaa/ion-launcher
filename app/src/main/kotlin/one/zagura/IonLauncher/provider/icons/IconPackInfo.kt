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

class IconPackInfo(
    private val res: Resources,
    private val iconPackPackageName: String,
) {
    class IconGenInfo(val res: Resources) {
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var scaleFactor = 1f
    }

    val iconResourceNames = HashMap<String, String>()
    val calendarPrefixes = HashMap<String, String>()
    var iconModificationInfo: IconGenInfo? = null

    @SuppressLint("DiscouragedApi")
    fun getDrawableResource(packageName: String, name: String): Int {
        val key = "ComponentInfo{$packageName/$name}"
        val iconResource = calendarPrefixes[key]
            ?.let { it + Calendar.getInstance()[Calendar.DAY_OF_MONTH] }
            ?: iconResourceNames[key]
            ?: return 0
        return res.getIdentifier(
            iconResource,
            "drawable",
            iconPackPackageName
        )
    }

    fun getDrawable(packageName: String, name: String, density: Int): Drawable? {
        val drawableRes = getDrawableResource(packageName, name)
        if (drawableRes == 0) return null
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
                        Intent(Intent.ACTION_MAIN).addCategory(category), 0)
                    for (a in activities) {
                        val activity = a.activityInfo
                        map["ComponentInfo{${activity.packageName}/${activity.name}}"] = value
                    }
                }
                else -> map[key] = value
            }
        }

        private fun loadIconMod(name: String, res: Resources, iconPackPackageName: String, info: IconPackInfo, uniformOptions: BitmapFactory.Options): Bitmap? {
            val i = res.getIdentifier(
                name,
                "drawable",
                iconPackPackageName
            )
            if (i == 0)
                return null
            if (info.iconModificationInfo == null)
                info.iconModificationInfo = IconGenInfo(res)
            return BitmapFactory.decodeResource(res, i, uniformOptions)
        }

        @SuppressLint("DiscouragedApi")
        fun get(
            packageManager: PackageManager,
            iconPackPackageName: String,
        ): IconPackInfo {
            val res = packageManager.getResourcesForApplication(iconPackPackageName)
            val info = IconPackInfo(res, iconPackPackageName)
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
                                    if (info.iconModificationInfo == null)
                                        info.iconModificationInfo = IconGenInfo(res)
                                    info.iconModificationInfo!!.scaleFactor = x.getAttributeValue(0).toFloat()
                                }
                                "item" -> {
                                    val key = x.getAttributeValue(null, "component")
                                    val value = x.getAttributeValue(null, "drawable")
                                    if (key != null && value != null)
                                        parseEntry(packageManager, info.iconResourceNames, key, value)
                                }
                                "calendar" -> {
                                    val key = x.getAttributeValue(null, "component")
                                    val value = x.getAttributeValue(null, "prefix")
                                    if (key != null && value != null)
                                        parseEntry(packageManager, info.calendarPrefixes, key, value)
                                }
                                "iconback" -> loadIconMod(
                                    x.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    info,
                                    uniformOptions
                                ).let { info.iconModificationInfo!!.back = it }
                                "iconmask" -> loadIconMod(
                                    x.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    info,
                                    uniformOptions
                                ).let { info.iconModificationInfo!!.mask = it }
                                "iconupon" -> loadIconMod(
                                    x.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    info,
                                    uniformOptions
                                ).let { info.iconModificationInfo!!.front = it }
                            }
                        } catch (_: Exception) {}
                    }
                    x.next()
                }
            } catch (e: Exception) { e.printStackTrace() }
            return info
        }

        @SuppressLint("DiscouragedApi")
        fun getResourceNames(res: Resources, packageName: String): List<String> {
            val strings = ArrayList<String>()
            try {
                val n = res.getIdentifier("drawable", "xml", packageName)
                if (n != 0) {
                    val xrp = res.getXml(n)
                    while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                        try {
                            if (xrp.eventType == 2 && !strings.contains(xrp.getAttributeValue(0)))
                                if (xrp.name == "item")
                                    strings.add(xrp.getAttributeValue(0))
                        } catch (_: Exception) {}
                        xrp.next()
                    }
                } else {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isValidating = false
                    val xpp = factory.newPullParser()
                    val raw = res.assets.open("drawable.xml")
                    xpp.setInput(raw, null)
                    while (xpp!!.eventType != XmlPullParser.END_DOCUMENT) {
                        try {
                            if (xpp.eventType == 2 && !strings.contains(xpp.getAttributeValue(0)))
                                if (xpp.name == "item")
                                    strings.add(xpp.getAttributeValue(0))
                        } catch (_: Exception) {}
                        xpp.next()
                    }
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
}