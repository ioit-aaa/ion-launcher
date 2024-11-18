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
import one.zagura.IonLauncher.provider.items.AppLoader.compareLabels

object AppCategorizer : UpdatingResource<Map<AppCategorizer.AppCategory, List<App>>>() {

    enum class AppCategory {
        AllApps,

        System,
        Customization,

        Utilities,
        Commute,
        Wellbeing,

        Media,
        Audio,
        Image,

        Communication,
        News,

        Productivity,
        Games,
    }

    private var categories = HashMap<AppCategory, MutableList<App>>()

    override fun getResource(): Map<AppCategory, List<App>> = categories

    fun onAppsLoaded(context: Context, apps: List<App>) {
        categorizeAllApps(context, apps)
    }

    fun categorizeAllApps(context: Context, apps: List<App>) {
        val c = CategorizationContext(context)
        apps.forEach(c::processApp)
        c.joinBIntoAIfSmall(AppCategory.System, AppCategory.Customization)
        c.joinBIntoAIfSmall(AppCategory.Utilities, AppCategory.Wellbeing)
        c.joinBIntoAIfSmall(AppCategory.Utilities, AppCategory.Commute)
        c.joinBIntoAIfSmall(AppCategory.Communication, AppCategory.News)
        c.ifSmallJoinTogether(AppCategory.Audio, AppCategory.Image, AppCategory.Media)
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
            val l = LabelLoader.loadLabel(context, app)
            val i = apps.binarySearch { compareLabels(LabelLoader.loadLabel(context, it), l) }
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

        private val utilityPackages = getForCategory(Intent.CATEGORY_APP_CALCULATOR).map { it.activityInfo.packageName }.toSet()
        private val wellbeingPackages = getForCategory(Intent.CATEGORY_APP_FITNESS).map { it.activityInfo.packageName }.toSet()
        private val productivityPackages = getForCategory(Intent.CATEGORY_APP_CALENDAR).map { it.activityInfo.packageName }.toSet()

        private val imagePackages = (getForCategory(Intent.CATEGORY_APP_GALLERY).asSequence() +
                getForAction(Intent.ACTION_CAMERA_BUTTON)).map { it.activityInfo.packageName }.toSet()

        private val audioPackages = getForCategory(Intent.CATEGORY_APP_MUSIC).map { it.activityInfo.packageName }.toSet()

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
            try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (info.category) {
                        ApplicationInfo.CATEGORY_GAME -> categories.add(AppCategory.Games)
                        ApplicationInfo.CATEGORY_IMAGE -> categories.add(AppCategory.Image)
                        ApplicationInfo.CATEGORY_VIDEO -> categories.add(AppCategory.Image)
                        ApplicationInfo.CATEGORY_AUDIO -> categories.add(AppCategory.Audio)
                        ApplicationInfo.CATEGORY_SOCIAL -> categories.add(AppCategory.Communication)

                        ApplicationInfo.CATEGORY_NEWS -> categories.add(AppCategory.News)
                        ApplicationInfo.CATEGORY_MAPS -> categories.add(AppCategory.Commute)

                        ApplicationInfo.CATEGORY_PRODUCTIVITY -> categories.add(AppCategory.Productivity)
                        ApplicationInfo.CATEGORY_ACCESSIBILITY -> categories.add(AppCategory.Utilities)
                        ApplicationInfo.CATEGORY_UNDEFINED -> {}
                    }
                } else @Suppress("DEPRECATION") {
                    if (info.flags and ApplicationInfo.FLAG_IS_GAME == ApplicationInfo.FLAG_IS_GAME)
                        categories.add(AppCategory.Games)
                }
            } catch (_: Exception) {}
            if (commutePackages.contains(packageName))
                categories.add(AppCategory.Commute)
            if (commPackages.contains(packageName))
                categories.add(AppCategory.Communication)
            if (utilityPackages.contains(packageName))
                categories.add(AppCategory.Utilities)
            if (productivityPackages.contains(packageName))
                categories.add(AppCategory.Productivity)
            if (audioPackages.contains(packageName))
                categories.add(AppCategory.Audio)
            if (imagePackages.contains(packageName))
                categories.add(AppCategory.Image)
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

        fun ifSmallJoinTogether(a: AppCategory, b: AppCategory, new: AppCategory) {
            val aa = categories[a] ?: return
            val bb = categories[b] ?: return
            if (aa.size < 3 || bb.size < 3) {
                val dest = categories.getOrPut(new, ::ArrayList)
                for (app in aa) if (app !in dest)
                    dest.add(app)
                for (app in bb) if (app !in dest)
                    dest.add(app)
                categories.remove(a)
                categories.remove(b)
            }
        }
    }
}