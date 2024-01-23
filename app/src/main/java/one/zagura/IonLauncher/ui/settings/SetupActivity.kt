package one.zagura.IonLauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.HomeScreen
import one.zagura.IonLauncher.ui.settings.common.color
import one.zagura.IonLauncher.ui.settings.common.permissionSwitch
import one.zagura.IonLauncher.ui.settings.common.setSettingsContentView
import one.zagura.IonLauncher.ui.settings.common.setting
import one.zagura.IonLauncher.ui.settings.common.title
import one.zagura.IonLauncher.util.Utils

class SetupActivity : Activity() {

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
                    updateUsageAccess = permissionSwitch(SuggestionsManager.checkUsageAccessPermission(view.context), ::grantUsageAccess)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setting(R.string.notification_access, subtitle = R.string.to_show_media_player) {
                    updateNotificationAccess = permissionSwitch(hasNotificationAccess(), ::grantNotificationAccess)
                }
            }
            title(R.string.look_and_feel)
            setting(R.string.background) {
                color("color:bg", ColorThemer.DEFAULT_BG)
            }
            setting(R.string.foreground) {
                color("color:fg", ColorThemer.DEFAULT_FG)
            }
            val dp = resources.displayMetrics.density
            val m = (18 * dp).toInt()
            view.addView(TextView(view.context).apply {
                setText(R.string.finish_setup)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val r = 12 * dp
                background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
                backgroundTintList = ColorStateList.valueOf(ColorThemer.COLOR_TEXT)
                setTextColor(ColorThemer.COLOR_CARD)
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
                background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
                backgroundTintList = ColorStateList.valueOf(ColorThemer.COLOR_TEXT)
                setTextColor(ColorThemer.COLOR_CARD)
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
            updateUsageAccess(SuggestionsManager.checkUsageAccessPermission(this))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            updateNotificationAccess(hasNotificationAccess())
        }
    }

    private fun start(v: View) {
        packageManager.setComponentEnabledSetting(ComponentName(this, SettingsActivity::class.java), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)

        if (packageManager.getComponentEnabledSetting(ComponentName(this, HomeScreen::class.java)) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, HomeScreen::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Dock.setItem(this, 0, getCategoryItem(Intent.CATEGORY_APP_CONTACTS))
            Dock.setItem(this, 1, getCategoryItem(Intent.CATEGORY_APP_CALCULATOR))
            Dock.setItem(this, 2, getCategoryItem(Intent.CATEGORY_APP_BROWSER))
            Dock.setItem(this, 3, getCategoryItem(Intent.CATEGORY_APP_CALENDAR))
            Dock.setItem(this, 4, getCategoryItem(Intent.CATEGORY_APP_GALLERY))
            Dock.setItem(this, 5, getCategoryItem(Intent.CATEGORY_APP_MAPS))
            Dock.setItem(this, 6, getCategoryItem(Intent.CATEGORY_APP_MUSIC))
            Dock.setItem(this, 7, getCategoryItem(Intent.CATEGORY_APP_EMAIL))
            Dock.setItem(this, 8, getCategoryItem(Intent.CATEGORY_APP_MARKET))
        }

        if (Utils.getDefaultLauncher(packageManager) != BuildConfig.APPLICATION_ID)
            Utils.chooseDefaultLauncher(this)
    }

    private fun getCategoryItem(category: String): LauncherItem? {
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN)
                .addCategory(category),
            PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.let {
            AppLoader.loadApp(this, it.packageName, it.name, Process.myUserHandle())
        }
    }

    private fun hasNotificationAccess() = NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(applicationContext.packageName)
    private fun hasContactsAccess() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    private fun hasCalendarAccess() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

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