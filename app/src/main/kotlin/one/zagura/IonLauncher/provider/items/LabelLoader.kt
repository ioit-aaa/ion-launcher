package one.zagura.IonLauncher.provider.items

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.provider.ContactsContract
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.ActionItem
import one.zagura.IonLauncher.data.items.OpenAlarmsItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.data.items.TorchToggleItem

object LabelLoader {
    private val cacheApps = HashMap<App, String>()
    private val cacheContacts = HashMap<String, String>() // only use lookup key
    private val cacheShortcuts = HashMap<StaticShortcut, String>()

    @SuppressLint("NewApi")
    fun loadLabel(context: Context, item: LauncherItem): String = when (item) {
        is App -> loadLabel(context, item)
        is ContactItem -> loadLabel(context, item)
        is ActionItem -> loadLabel(context, item)
        is StaticShortcut -> loadLabel(context, item)
        is TorchToggleItem -> context.getString(R.string.turn_off_torch)
        is OpenAlarmsItem -> context.getString(R.string.alarms)
    }

    fun loadLabel(context: Context, item: ActionItem): String {
        val i = context.packageManager.queryIntentActivities(Intent(item.action), 0)
            .firstOrNull() ?: return ""
        return i.loadLabel(context.packageManager).toString()
    }

    fun loadLabel(context: Context, app: App): String {
        return cacheApps.getOrPut(app) {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.getActivityList(app.packageName, app.userHandle)
                ?.find { it.name == app.name }
                ?.label?.toString() ?: app.packageName
        }
    }

    fun loadLabel(context: Context, contact: ContactItem): String {
        return cacheContacts.getOrPut(contact.lookupKey) {
            val cur = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ), "${ContactsContract.Contacts.LOOKUP_KEY}=?", arrayOf(contact.lookupKey), null)
                ?: return ""
            var name = ""
            if (cur.moveToNext())
                name = cur.getString(1)
            cur.close()
            val nicknameCur = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Nickname.NAME,
                    ContactsContract.Data.LOOKUP_KEY),
                "${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.Data.LOOKUP_KEY}=?",
                arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE, contact.lookupKey), null)

            if (nicknameCur != null) {
                if (nicknameCur.moveToNext()) {
                    val nickname = nicknameCur.getString(0)
                    if (nickname != null)
                        name = nickname
                }
                nicknameCur.close()
            }

            name
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun loadLabel(context: Context, shortcut: StaticShortcut): String {
        return cacheShortcuts.getOrPut(shortcut) {
            shortcut.getShortcutInfo(context)?.shortLabel?.toString() ?: ""
        }
    }

    fun loadLabel(context: Context, packageName: String, userHandle: UserHandle): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.getApplicationInfo(packageName, 0, userHandle)
                .loadLabel(context.packageManager).toString()
        } else
            context.packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(context.packageManager).toString()
    }

    fun clearCache() {
        cacheApps.clear()
        cacheContacts.clear()
        cacheShortcuts.clear()
    }

    fun removePackage(packageName: String) {
        val iterator = cacheApps.iterator()
        for ((app, _) in iterator)
            if (app.packageName == packageName)
                iterator.remove()
    }
}
