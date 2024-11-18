package one.zagura.IonLauncher.provider.items

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.IonLauncherApp
import one.zagura.IonLauncher.util.TaskRunner
import java.util.TreeSet
import kotlin.math.min

object AppLoader : UpdatingResource<List<App>>() {

    /** Sorted */
    override fun getResource(): List<App> = apps

    private var apps: MutableList<App> = ArrayList()

    fun reloadApps(ionApplication: IonLauncherApp) {
        TaskRunner.submit {
            val userManager = ionApplication.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = ionApplication.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collection = TreeSet<App> { a, b ->
                val la = LabelLoader.loadLabel(ionApplication, a)
                val lb = LabelLoader.loadLabel(ionApplication, b)
                val c = compareLabels(la, lb)
                if (c == 0) a.compareTo(b) else c
            }

            for (user in userManager.userProfiles) {
                val appList = launcherApps.getActivityList(null, user)
                for (i in appList.indices) {
                    val packageName = appList[i].applicationInfo.packageName
                    val name = appList[i].name
                    val app = App(packageName, name, user)
                    if (!HiddenApps.isHidden(ionApplication.settings, app))
                        collection.add(app)
                }
            }
            apps = collection.toMutableList()
            update(apps)
            SuggestionsManager.onAppsLoaded(ionApplication)
            AppCategorizer.onAppsLoaded(ionApplication, apps)
        }
    }

    fun loadApp(
        context: Context,
        packageName: String,
        name: String,
        user: UserHandle,
    ): App? {
        return apps.find { it.packageName == packageName && it.name == name && it.userHandle == user } ?: run {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            if (launcherApps.getActivityList(packageName, user)?.any { it.name == name } != true)
                return null
            App(packageName, name, user)
        }
    }

    fun onHide(app: App) {
        apps.remove(app)
        update(apps)
        AppCategorizer.onHide(app)
    }

    fun onShow(context: Context, app: App) {
        val l = LabelLoader.loadLabel(context, app)
        val i = apps.binarySearch { compareLabels(LabelLoader.loadLabel(context, it), l) }
        if (i < 0)
            apps.add(-i - 1, app)
        update(apps)
        AppCategorizer.onShow(context, app)
    }

    class AppCallback(
        val ionApplication: IonLauncherApp,
    ) : LauncherApps.Callback() {

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            apps.removeAll { it.packageName == packageName && it.userHandle == user }
            AppCategorizer.onAppUninstalled(packageName, user)
            SuggestionsManager.onAppUninstalled(packageName, user)
            IconLoader.removePackage(packageName)
            LabelLoader.removePackage(packageName)
            update(apps)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reloadApps(ionApplication)

        override fun onPackageChanged(packageName: String, user: UserHandle?) {
            reloadApps(ionApplication)
            IconLoader.removePackage(packageName)
            LabelLoader.removePackage(packageName)
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reloadApps(ionApplication)

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reloadApps(ionApplication)

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = reloadApps(ionApplication)

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = reloadApps(ionApplication)
    }

    private const val MAX_LATIN = '\u024F'

    fun compareLabels(s1: String, s2: String): Int {
        val n1 = s1.length
        val n2 = s2.length
        val min = min(n1, n2)
        for (i in 0 until min) {
            var c1 = s1[i]
            var c2 = s2[i]
            // Non-latin labels go first to avoid untranslated
            // apps pushing translated ones down in the app drawer
            if (MAX_LATIN in c2..<c1) return -1
            if (MAX_LATIN in c1..<c2) return 1
            if (c1 != c2) {
                c1 = c1.lowercaseChar()
                c2 = c2.lowercaseChar()
                if (c1 != c2)
                    return c1.code - c2.code
            }
        }
        return n1 - n2
    }
}