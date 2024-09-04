package one.zagura.IonLauncher.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.ContactsLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.HomeScreen
import one.zagura.IonLauncher.ui.view.settings.permissionSwitch
import one.zagura.IonLauncher.ui.view.settings.setSettingsContentView
import one.zagura.IonLauncher.ui.view.settings.setting
import one.zagura.IonLauncher.ui.view.settings.title
import one.zagura.IonLauncher.util.Utils
import one.zagura.IonLauncher.util.drawable.UniformSquircleRectShape

class SetupActivity : Activity() {

    companion object {
        private const val SETUP_ALIAS = BuildConfig.APPLICATION_ID + ".SetupAlias"
        private const val LAUNCHER_ALIAS = BuildConfig.APPLICATION_ID + ".LauncherAlias"
    }

    private lateinit var updateContactsAccess: (Boolean) -> Unit
    private lateinit var updateCalendarAccess: (Boolean) -> Unit
    private lateinit var updateUsageAccess: (Boolean) -> Unit
    private lateinit var updateNotificationAccess: (Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSettingsContentView(R.string.app_name) {
            title(R.string.grant_permissions)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setting(R.string.contacts_access, subtitle = R.string.to_show_in_search) {
                    updateContactsAccess = permissionSwitch(hasContactsAccess(), ::grantContactsAccess)
                }
                setting(R.string.calendar_access, subtitle = R.string.to_show_upcoming_events) {
                    updateCalendarAccess = permissionSwitch(hasCalendarAccess(), ::grantCalendarAccess)
                }
                setting(R.string.usage_access, subtitle = R.string.for_suggestions) {
                    updateUsageAccess = permissionSwitch(SuggestionsManager.hasPermission(view.context), ::grantUsageAccess)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setting(R.string.notification_access, subtitle = R.string.to_show_media_player) {
                    updateNotificationAccess = permissionSwitch(hasNotificationAccess(), ::grantNotificationAccess)
                }
            }
            val dp = resources.displayMetrics.density
            val m = (18 * dp).toInt()
            view.addView(TextView(view.context).apply {
                setText(R.string.finish_setup)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val r = 12 * dp
                background = ShapeDrawable(UniformSquircleRectShape(r))
                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.color_text))
                setTextColor(resources.getColor(R.color.color_bg))
                setOnClickListener(::start)
            }, ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * dp).toInt()).apply {
                setMargins(m, (32 * dp).toInt(), m, m)
            })
            view.addView(TextView(view.context).apply {
                setText(R.string.just_look)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val r = 12 * dp
                background = ShapeDrawable(UniformSquircleRectShape(r))
                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.color_text))
                setTextColor(resources.getColor(R.color.color_bg))
                setOnClickListener {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(it.context, HomeScreen::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                    Dock.setItem(it.context, 0, AppLoader.loadApp(
                        it.context,
                        BuildConfig.APPLICATION_ID,
                        SetupActivity::class.java.name,
                        Process.myUserHandle(),
                    ))
                    it.context.startActivity(Intent(it.context, HomeScreen::class.java))
                }
            }, ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * dp).toInt()).apply {
                setMargins(m, 0, m, m)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            updateContactsAccess(hasContactsAccess())
            updateCalendarAccess(hasCalendarAccess())
            updateUsageAccess(SuggestionsManager.hasPermission(this))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            updateNotificationAccess(hasNotificationAccess())
        }
    }

    private fun start(v: View) {
        if (packageManager.getComponentEnabledSetting(ComponentName(this, LAUNCHER_ALIAS)) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, HomeScreen::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            @SuppressLint("InlinedApi")
            val items = listOfNotNull(
                getCategoryItem(Intent.CATEGORY_APP_BROWSER),
                getCategoryItem(Intent.CATEGORY_APP_CALCULATOR),
                getCategoryItem(Intent.CATEGORY_APP_CALENDAR),
                getCategoryItem(Intent.CATEGORY_APP_CONTACTS),
                getCategoryItem(Intent.CATEGORY_APP_EMAIL),
                getCategoryItem(Intent.CATEGORY_APP_FILES),
                getCategoryItem(Intent.CATEGORY_APP_FITNESS),
                getCategoryItem(Intent.CATEGORY_APP_GALLERY),
                getCategoryItem(Intent.CATEGORY_APP_MAPS),
                getCategoryItem(Intent.CATEGORY_APP_MARKET),
                getCategoryItem(Intent.CATEGORY_APP_MESSAGING),
                getCategoryItem(Intent.CATEGORY_APP_MUSIC),
                getCategoryItem(Intent.CATEGORY_APP_WEATHER),
            )
            for ((i, item) in items.withIndex()) {
                Dock.setItem(this, i, item)
            }
        }

        packageManager.setComponentEnabledSetting(ComponentName(this, SETUP_ALIAS), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(ComponentName(this, LAUNCHER_ALIAS), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        Utils.chooseDefaultLauncher(this)
    }

    private fun getCategoryItem(category: String): LauncherItem? {
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.let {
            AppLoader.loadApp(this, it.packageName, it.name, Process.myUserHandle())
        }
    }

    private fun hasNotificationAccess() = NotificationService.hasPermission(this)
    private fun hasContactsAccess() = ContactsLoader.hasPermission(this)
    private fun hasCalendarAccess() = EventsLoader.hasPermission(this)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun grantNotificationAccess(v: View) {
        applicationContext.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantUsageAccess(v: View) =
        applicationContext.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantContactsAccess(v: View) =
        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 0)

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantCalendarAccess(v: View) =
        requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 0)
}