package one.zagura.IonLauncher.provider.icons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.ActionItem
import one.zagura.IonLauncher.data.items.OpenAlarmsItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.data.items.TorchToggleItem
import one.zagura.IonLauncher.provider.EditedItems
import one.zagura.IonLauncher.util.drawable.NonDrawable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils.setGrayscale
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object IconLoader {
    private val cacheApps = HashMap<App, Drawable>()
    private val cacheContacts = HashMap<String, Drawable>() // only use lookup key
    private val cacheShortcuts = HashMap<StaticShortcut, Drawable>()
    private var iconPacks = emptyList<IconPackInfo>()

    private val iconPacksLock = ReentrantLock()

    fun updateIconPacks(context: Context, settings: Settings) {
        IconThemer.updateSettings(context, settings)
        TaskRunner.submit {
            iconPacksLock.withLock {
                internalClearCache()
                iconPacks = settings.getStrings("icon_packs").orEmpty().mapNotNull { p ->
                    try {
                        IconPackInfo.get(context.packageManager, p)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
    }

    private fun internalClearCache() {
        cacheApps.clear()
        cacheContacts.clear()
        cacheShortcuts.clear()
    }

    fun clearCache() = iconPacksLock.withLock {
        internalClearCache()
    }

    fun removePackage(packageName: String) {
        val iterator = cacheApps.iterator()
        for ((app, _) in iterator)
            if (app.packageName == packageName)
                iterator.remove()
    }

    fun invalidateItem(item: LauncherItem) {
        when (item) {
            is App -> cacheApps.remove(item)
            is ContactItem -> cacheContacts.remove(item.lookupKey)
            is StaticShortcut -> cacheShortcuts.remove(item)
            else -> {}
        }
    }

    @SuppressLint("NewApi")
    fun loadIcon(context: Context, item: LauncherItem): Drawable = when (item) {
        is App -> loadIcon(context, item)
        is ContactItem -> loadIcon(context, item)
        is ActionItem -> loadIcon(context, item)
        is StaticShortcut -> loadIcon(context, item)
        is TorchToggleItem -> IconThemer.loadSymbolicIcon(context, R.drawable.torch)
        is OpenAlarmsItem -> IconThemer.loadSymbolicIcon(context, R.drawable.alarm)
    }

    private fun loadIcon(context: Context, item: ActionItem): Drawable {
        val initIcon = context.packageManager.queryIntentActivities(Intent(item.action), 0)
            .firstOrNull()?.loadIcon(context.packageManager) ?: return NonDrawable
        return iconPacksLock.withLock {
            IconThemer.applyTheming(context, initIcon, iconPacks)
        }
    }

    private fun loadIcon(context: Context, app: App): Drawable {
        return cacheApps.getOrPut(app) {
            context.packageManager.getUserBadgedIcon(iconPacksLock.withLock {
                val custom = EditedItems.getIcon(context, app)
                if (custom != null)
                    return@withLock IconThemer.transformIconFromIconPack(custom)
                val externalIcon = getIconPackIcon(context, app.packageName, app.name)
                if (externalIcon != null)
                    return@withLock IconThemer.transformIconFromIconPack(externalIcon)
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                launcherApps.getActivityList(app.packageName, app.userHandle)
                    ?.find { it.name == app.name }
                    ?.getIcon(context.resources.displayMetrics.densityDpi)
                    ?.let { IconThemer.applyTheming(context, it, iconPacks) }
                    ?: return@withLock NonDrawable
            }, app.userHandle)
        }
    }

    private fun loadIcon(context: Context, contact: ContactItem): Drawable {
        return cacheContacts.getOrPut(contact.lookupKey) {
            if (contact.iconUri != null) try {
                val inputStream = context.contentResolver.openInputStream(contact.iconUri)
                val pic = Drawable.createFromStream(inputStream, contact.iconUri.toString())
                    ?: return@getOrPut NonDrawable
                return@getOrPut IconThemer.iconifyQuadImage(pic)
            } catch (_: FileNotFoundException) {}
            val realName = LabelLoader.loadLabel(context, contact).trim()
            if (realName.isEmpty())
                return@getOrPut NonDrawable
            IconThemer.makeContact(realName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun loadIcon(context: Context, shortcut: StaticShortcut): Drawable {
        return cacheShortcuts.getOrPut(shortcut) {
            val custom = EditedItems.getIcon(context, shortcut)
            if (custom != null)
                return@getOrPut IconThemer.transformIconFromIconPack(custom)
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            if (!launcherApps.hasShortcutHostPermission())
                return@getOrPut NonDrawable
            val icon = launcherApps.getShortcutIconDrawable(
                shortcut.getShortcutInfo(context) ?: return@getOrPut NonDrawable,
                context.resources.displayMetrics.densityDpi
            ) ?: return@getOrPut NonDrawable
            icon.setGrayscale(IconThemer.doGrayscale)
            icon
        }
    }

    private fun getIconPackIcon(context: Context, packageName: String, name: String) = iconPacks.firstNotNullOfOrNull {
        it.getDrawable(packageName, name, context.resources.displayMetrics.densityDpi)
    }
}
