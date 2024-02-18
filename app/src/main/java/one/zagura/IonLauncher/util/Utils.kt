package one.zagura.IonLauncher.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.ui.settings.FakeLauncherActivity
import java.util.Calendar


object Utils {
    fun startDrag(view: View, item: LauncherItem, localState: Any?) {
        val shadow = ItemDragShadow(view.context, IconLoader.loadIcon(view.context, item))
        val clipData = ClipData(
            item.toString(),
            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            ClipData.Item(item.toString()),
        )

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            view.startDragAndDrop(
                clipData,
                shadow,
                localState,
                View.DRAG_FLAG_OPAQUE or View.DRAG_FLAG_GLOBAL
            )
        else view.startDrag(clipData, shadow, localState, 0)
    }

    @Suppress("DEPRECATION")
    fun setDarkStatusFG(window: Window, isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val f = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (isDark) f else 0, f)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = if (isDark) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (isDark) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
        }
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    fun getStatusBarHeight(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.statusBars()).top
        } else {
            val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (id > 0) context.resources.getDimensionPixelSize(id) else 0
        }
    }

    fun getNavigationBarHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            val display = try {
                context.display
            } catch (e: NoSuchMethodError) {
                windowManager.defaultDisplay
            } ?: return 0
            val appUsableSize = Point()
            val realScreenSize = Point()
            with(display) {
                getSize(appUsableSize)
                getRealSize(realScreenSize)
            }
            // navigation bar on the side
            if (appUsableSize.x < realScreenSize.x)
                return realScreenSize.x - appUsableSize.x

            // navigation bar at the bottom
            return if (appUsableSize.y < realScreenSize.y)
                realScreenSize.y - appUsableSize.y
            else 0
        }
    }

    fun getDisplayHeight(activity: Activity): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.maximumWindowMetrics.bounds.height()
        } else {
            val realMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getRealMetrics(realMetrics)
            realMetrics.heightPixels
        }

    fun getDisplayWidth(activity: Activity): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.maximumWindowMetrics.bounds.width()
        } else {
            val realMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getRealMetrics(realMetrics)
            realMetrics.widthPixels
        }

    fun pullStatusBar(context: Context) {
        try {
            @SuppressLint("WrongConstant")
            val sbs = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel")(sbs)
        } catch (e: Exception) { e.printStackTrace() }
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun vibrateDrop(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val v = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            if (v.defaultVibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
                v.vibrate(
                    CombinedVibration.createParallel(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD)
                            .compose()),
                    VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
                return
            }
        }
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(
                VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else v.vibrate(15L)
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun click(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(
                VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else v.vibrate(10L)
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun tick(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            v.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(
                VibrationEffect.createOneShot(1L, VibrationEffect.DEFAULT_AMPLITUDE),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        else v.vibrate(1L)
    }

    fun chooseDefaultLauncher(context: Context) {
        val componentName = ComponentName(context, FakeLauncherActivity::class.java)
        context.packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(selector)
        context.packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
    }

    fun getDefaultLauncher(packageManager: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.resolvePackageName
    }

    fun format(context: Context, time: Long): String =
        DateFormat.getTimeFormat(context).format(Calendar.getInstance().apply {
            timeInMillis = time
        }.time)
}