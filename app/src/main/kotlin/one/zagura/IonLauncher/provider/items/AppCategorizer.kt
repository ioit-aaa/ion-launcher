package one.zagura.IonLauncher.provider.items

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.UpdatingResource
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.LabelLoader

object AppCategorizer : UpdatingResource<Map<AppCategorizer.AppCategory, List<App>>>() {

    enum class AppCategory {
        AllApps,
        System,
        Customization,
        Utilities,
        Media,
        Communication,
        Productivity,
        Wellbeing,
        Commute,
        Games,
    }

    private var categories = HashMap<AppCategory, MutableList<App>>()

    override fun getResource(): Map<AppCategory, List<App>> = categories

    fun onAppsLoaded(context: Context, apps: MutableList<App>) {
        categorizeAllApps(context, apps)
    }

    fun categorizeAllApps(context: Context, apps: MutableList<App>) {
        val c = CategorizationContext(context)
        apps.forEach(c::processApp)
        c.joinBIntoAIfSmall(AppCategory.System, AppCategory.Customization)
        c.joinBIntoAIfSmall(AppCategory.Utilities, AppCategory.Wellbeing)
        c.joinBIntoAIfSmall(AppCategory.Utilities, AppCategory.Commute)
        categories = c.categories
        update(categories)
    }

    fun onAppUninstalled(packageName: String, user: UserHandle) {
        categories.forEach { (_, l) -> l.removeAll { it.packageName == packageName && it.userHandle == user } }
        update(categories)
    }

    fun onHide(app: App) {
        categories.forEach { (_, c) ->
            c.remove(app)
        }
        update(categories)
    }

    fun onShow(context: Context, app: App) {
        for ((c, _) in CategorizationContext(context).apply { processApp(app) }.categories) {
            val apps = categories.getOrPut(c, ::ArrayList)
            val i = apps.binarySearchBy(LabelLoader.loadLabel(context, app).lowercase()) { LabelLoader.loadLabel(context, it).lowercase() }
            if (i < 0)
                apps.add(-i - 1, app)
        }
        update(categories)
    }

    class CategorizationContext(val context: Context) {

        private val systemPackages = (getForCategory(Intent.CATEGORY_APP_MARKET) +
                getForCategory(Intent.CATEGORY_APP_FILES) +
                getForCategory(Intent.CATEGORY_APP_BROWSER) +
                getForAction(Settings.ACTION_SETTINGS)).map { it.activityInfo.packageName }.toSet()

        private val customizationPackages = (IconPackInfo.getAvailableIconPacks(context.packageManager).asSequence() +
                getForCategory(Intent.CATEGORY_HOME)).map { it.activityInfo.packageName }.toSet()

        private val commutePackages = (getForCategory(Intent.CATEGORY_APP_MAPS).asSequence() +
                getForCategory(Intent.CATEGORY_APP_WEATHER) +
                getForCategory(Intent.CATEGORY_APP_FITNESS)).map { it.activityInfo.packageName }.toSet()

        private val utilityPackages = getForCategory(Intent.CATEGORY_APP_CALCULATOR).map { it.activityInfo.packageName }
        private val wellbeingPackages = getForCategory(Intent.CATEGORY_APP_FITNESS).map { it.activityInfo.packageName }
        private val productivityPackages = getForCategory(Intent.CATEGORY_APP_CALENDAR).map { it.activityInfo.packageName }

        private val mediaPackages = (getForCategory(Intent.CATEGORY_APP_GALLERY).asSequence() +
                getForCategory(Intent.CATEGORY_APP_MUSIC) +
                getForAction(Intent.ACTION_CAMERA_BUTTON)).map { it.activityInfo.packageName }.toSet()

        private val commPackages = (getForCategory(Intent.CATEGORY_APP_EMAIL).asSequence() +
                getForCategory(Intent.CATEGORY_APP_MESSAGING) +
                getForCategory(Intent.CATEGORY_APP_CONTACTS) +
                getForCategory(Intent.ACTION_CALL) +
                getForCategory(Intent.ACTION_DIAL)).map { it.activityInfo.packageName }.toSet()

        val categories = HashMap<AppCategory, MutableList<App>>(AppCategory.entries.size)

        fun processApp(app: App) {
            val pc = categorizePackage(app.packageName)
            for (x in pc)
                categories.getOrPut(x, ::ArrayList).add(app)
        }

        private fun categorizePackage(packageName: String): HashSet<AppCategory> {
            val categories = HashSet<AppCategory>()
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (info.category) {
                    ApplicationInfo.CATEGORY_GAME -> categories.add(AppCategory.Games)
                    ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> categories.add(AppCategory.Media)
                    ApplicationInfo.CATEGORY_SOCIAL -> categories.add(AppCategory.Communication)

                    ApplicationInfo.CATEGORY_NEWS -> categories.add(AppCategory.Communication) // hmmm
                    ApplicationInfo.CATEGORY_MAPS -> categories.add(AppCategory.Commute)

                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> categories.add(AppCategory.Productivity)
                    ApplicationInfo.CATEGORY_ACCESSIBILITY -> categories.add(AppCategory.Utilities)
                    ApplicationInfo.CATEGORY_UNDEFINED -> {}
                }
            } else @Suppress("DEPRECATION") {
                if (info.flags and ApplicationInfo.FLAG_IS_GAME == ApplicationInfo.FLAG_IS_GAME)
                    categories.add(AppCategory.Games)
            }
            if (commutePackages.contains(packageName))
                categories.add(AppCategory.Commute)
            if (commPackages.contains(packageName))
                categories.add(AppCategory.Communication)
            if (utilityPackages.contains(packageName))
                categories.add(AppCategory.Utilities)
            if (productivityPackages.contains(packageName))
                categories.add(AppCategory.Productivity)
            if (mediaPackages.contains(packageName))
                categories.add(AppCategory.Media)
            if (systemPackages.contains(packageName))
                categories.add(AppCategory.System)
            if (customizationPackages.contains(packageName))
                categories.add(AppCategory.Customization)
            if (wellbeingPackages.contains(packageName))
                categories.add(AppCategory.Wellbeing)
            return categories
        }

        private fun getForCategory(category: String) =
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
        private fun getForAction(action: String) =
            context.packageManager.queryIntentActivities(Intent(action), 0)

        fun joinBIntoAIfSmall(a: AppCategory, b: AppCategory) {
            val aa = categories[a] ?: return
            val bb = categories[b] ?: return
            if (aa.size < 3 || bb.size < 3) {
                for (app in bb) if (app !in aa)
                    aa.add(app)
                categories.remove(b)
            }
        }
    }
}