package one.zagura.IonLauncher.data.items

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.UserHandle
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N_MR1)
class StaticShortcut(
    val packageName: String,
    val id: String,
    val userHandle: UserHandle,
) : LauncherItem() {

    override fun open(view: View, bounds: Rect) {
        super.open(view, bounds)
        val anim = createOpeningAnimation(view, bounds.left, bounds.top, bounds.right, bounds.bottom).toBundle()
        val info = getShortcutInfo(view.context) ?: return
        val launcherApps = view.context.getSystemService(LauncherApps::class.java)
        launcherApps.startShortcut(info, null, anim)
    }

    override fun open(view: View) {
        super.open(view)
        val anim = createOpeningAnimation(view).toBundle()
        val info = getShortcutInfo(view.context) ?: return
        val launcherApps = view.context.getSystemService(LauncherApps::class.java)
        launcherApps.startShortcut(info, null, anim)
    }

    fun getShortcutInfo(context: Context): ShortcutInfo? {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
            .setPackage(packageName)
            .setShortcutIds(listOf(id))
        return try {
            launcherApps.getShortcuts(query, userHandle)?.firstOrNull()
        } catch (_: Exception) { null }
    }

    override fun toString() = "${SHORTCUT.toString(16)}$packageName/$id/${userHandle.hashCode().toString(16)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StaticShortcut
        if (id != other.id) return false
        return packageName == other.packageName
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

}