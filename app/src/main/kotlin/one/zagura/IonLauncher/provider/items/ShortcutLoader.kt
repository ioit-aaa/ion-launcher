package one.zagura.IonLauncher.provider.items

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.StaticShortcut

@RequiresApi(Build.VERSION_CODES.N_MR1)
object ShortcutLoader {

    fun getShortcut(context: Context, packageName: String, id: String, userHandle: UserHandle): StaticShortcut? {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
            .setPackage(packageName)
            .setShortcutIds(listOf(id))
        return try {
            launcherApps.getShortcuts(query, userHandle)?.firstOrNull()?.let {
                StaticShortcut(packageName, id, userHandle)
            }
        } catch (_: Exception) { null }
    }

    fun getStaticShortcuts(context: Context, app: App, out: MutableCollection<in StaticShortcut>) {
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
            .setPackage(app.packageName)
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        try {
            launcherApps.getShortcuts(query, Process.myUserHandle())?.forEach {
                out.add(StaticShortcut(app.packageName, it.id, app.userHandle))
            }
        } catch (_: Exception) {}
    }
}