package one.zagura.IonLauncher.provider.items

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.ionApplication

object AppLoader : UpdatingResource<List<App>>() {
    override fun getResource() = apps

    private var apps = ArrayList<App>()

    fun reloadApps(context: Context) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val collection = ArrayList<App>()

        for (user in userManager.userProfiles) {
            val appList = launcherApps.getActivityList(null, user)
            for (i in appList.indices) {
                val packageName = appList[i].applicationInfo.packageName
                val name = appList[i].name
                val label = appList[i].label.toString().ifEmpty { packageName }
                val app = App(packageName, name, user, label)
                if (!HiddenApps.isHidden(context.ionApplication.settings, app))
                    collection.add(app)
            }
        }
        apps = collection
        resort()
        update(apps)
        SuggestionsManager.onAppsLoaded(context)
    }

    private fun resort() {
        apps.sortBy { it.label }
    }


    fun loadApp(
        context: Context,
        packageName: String,
        name: String,
        user: UserHandle,
    ): App? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val info = launcherApps.getActivityList(packageName, user)?.find { it.name == name } ?: return null
        return App(packageName, name, user, info.label.toString())
    }

    fun onHide(app: App) {
        apps.remove(app)
        update(apps)
    }

    fun onShow(app: App) {
        if (!apps.contains(app))
            apps.add(app)
        resort()
        update(apps)
    }

    class AppCallback(
        val context: Context,
    ) : LauncherApps.Callback() {

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            apps.removeAll { it.packageName == packageName && it.userHandle == user }
            update(apps)
            SuggestionsManager.onAppUninstalled(context, packageName, user)
            IconLoader.removePackage(packageName)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reloadApps(context)

        override fun onPackageChanged(packageName: String, user: UserHandle?) {
            reloadApps(context)
            IconLoader.removePackage(packageName)
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reloadApps(context)

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reloadApps(context)

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = reloadApps(context)

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = reloadApps(context)
    }
}