package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Picture
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.record
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Widgets
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.notification.TopNotificationProvider
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.drawer.DrawerArea
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.ui.view.MediaView
import one.zagura.IonLauncher.ui.view.PinnedGridView
import one.zagura.IonLauncher.ui.view.SharedDrawingContext
import one.zagura.IonLauncher.ui.view.SuggestionRowView
import one.zagura.IonLauncher.ui.view.SummaryView
import one.zagura.IonLauncher.ui.view.WidgetView
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.FloatingIconView
import one.zagura.IonLauncher.util.GestureNavContract
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils
import org.lsposed.hiddenapibypass.HiddenApiBypass


class HomeScreen : Activity() {

    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var sheet: View
    private var sheetCallback: BottomSheetCallback = MinimalDrawerSheetCallback()

    private lateinit var homeScreen: CoordinatorLayout
    private lateinit var desktop: LinearLayout
    private lateinit var drawerArea: DrawerArea

    private lateinit var summaryView: SummaryView

    private lateinit var mediaView: MediaView

    private var widgetView: WidgetView? = null

    private lateinit var pinnedGrid: PinnedGridView
    private lateinit var suggestionsView: SuggestionRowView

    private lateinit var drawCtx: SharedDrawingContext

    private val screenBackground = FillDrawable(0)
    private var screenBackgroundColor = 0
    private var drawerBackgroundColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.setHiddenApiExemptions("L")

        drawCtx = SharedDrawingContext(this)
        homeScreen = createHomeScreen()
        val h = Utils.getDisplayHeight(this) * 2
        setContentView(homeScreen, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, h, Gravity.BOTTOM))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.setDecorFitsSystemWindows(false)
        else window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(0) {
                if (sheetBehavior.state != STATE_COLLAPSED)
                    sheetBehavior.state = STATE_COLLAPSED
            }

        homeScreen.background = screenBackground
        IconLoader.updateIconPacks(this, ionApplication.settings)
        applyCustomizations()
        SuggestionsManager.track {
            suggestionsView.update(it)
        }
        homeScreen.post {
            // windowToken might not be loaded, so we post this to the view
            val wallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            wallpaperManager.setWallpaperOffsetSteps(0f, 0f)
            wallpaperManager.setWallpaperOffsets(homeScreen.windowToken, 0.5f, 0.5f)
        }
    }

    private fun createHomeScreen(): CoordinatorLayout {
        val dp = resources.displayMetrics.density
        val fullHeight = Utils.getDisplayHeight(this)

        summaryView = SummaryView(this, drawCtx)
        mediaView = MediaView(this, drawCtx)
        suggestionsView = SuggestionRowView(this, drawCtx, ::showDropTargets, ::search)
        pinnedGrid = PinnedGridView(this, drawCtx)

        desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(
                summaryView,
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            addView(
                mediaView,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            addView(pinnedGrid,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, pinnedGrid.calculateGridHeight()))
            addView(
                suggestionsView,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, 0))
        }

        val offset = (256 * dp).toInt()
        drawerArea = DrawerArea(this, ::showDropTargets, ::onDrawerItemOpened).apply {
            setPadding(0, offset, 0, 0)
        }

        sheet = FrameLayout(this).apply {
            addView(desktop, LayoutParams(LayoutParams.MATCH_PARENT, fullHeight))
            addView(drawerArea, LayoutParams(LayoutParams.MATCH_PARENT, fullHeight + offset))
        }

        sheetBehavior = BottomSheetBehavior<View>().apply {
            isHideable = false
            peekHeight = fullHeight
            state = STATE_COLLAPSED
        }

        return CoordinatorLayout(this).apply {
            fitsSystemWindows = false
            addView(sheet, CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, fullHeight + offset).apply {
                behavior = sheetBehavior
            })
        }
    }

    inner class DrawerSheetCallback : BottomSheetCallback() {
        private val wallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
        private val setWallpaperZoomOut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) try {
            WallpaperManager::class.java
                .getDeclaredMethod("setWallpaperZoomOut", IBinder::class.java, Float::class.java)
                .apply { isAccessible = true }
        } catch (_: Exception) { null } else null
        private val offset = 256 * resources.displayMetrics.density * 0.4f

        override fun onStateChanged(view: View, newState: Int) =
            onDrawerStateChanged(view, newState)

        override fun onSlide(view: View, slideOffset: Float) {
            drawerArea.alpha = slideOffset * slideOffset / 0.6f - 0.4f
            val a = (slideOffset * 2f).coerceAtMost(1f)
            val inva = 1f - a
            screenBackground.color = ColorUtils.blendARGB(screenBackgroundColor, drawerBackgroundColor, a)
            desktop.alpha = inva
            desktop.translationY = a * offset
            val scale = 1f - a * 0.05f
            desktop.scaleX = scale
            desktop.scaleY = scale
            setWallpaperZoomOut?.invoke(wallpaperManager, homeScreen.windowToken, slideOffset)
        }
    }

    inner class MinimalDrawerSheetCallback : BottomSheetCallback() {
        override fun onStateChanged(view: View, newState: Int) =
            onDrawerStateChanged(view, newState)

        override fun onSlide(view: View, slideOffset: Float) {
            val a = (slideOffset * 1.5f).coerceAtMost(1f)
            screenBackground.color = ColorUtils.blendARGB(screenBackgroundColor, drawerBackgroundColor, a)
            drawerArea.alpha = a * a * a
            desktop.alpha = 1f - a
        }
    }

    fun onDrawerStateChanged(view: View, newState: Int) {
        if (newState == STATE_EXPANDED)
            Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.drawerForeground(this@HomeScreen)) < 0.5f)
        else
            drawerArea.clearSearch()
        desktop.isVisible = newState != STATE_EXPANDED
        if (newState == STATE_COLLAPSED) {
            drawerArea.isInvisible = true
            desktop.bringToFront()
            Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.wallForeground(this@HomeScreen)) < 0.5f)
        } else {
            drawerArea.isInvisible = false
            drawerArea.bringToFront()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SuggestionsManager.release()
    }

    override fun onStart() {
        super.onStart()
        ionApplication.settings.consumeUpdate {
            IconLoader.updateIconPacks(this, ionApplication.settings)
            suggestionsView.update(SuggestionsManager.getResource())
            NotificationService.MediaObserver.updateMediaItem(applicationContext)
            applyCustomizations()
        }
        AppLoader.track(false) {
            drawerArea.onAppsChanged()
            runOnUiThread {
                pinnedGrid.updateGridApps()
            }
        }
        Battery.track(false) {
            summaryView.updateAtAGlance()
        }
        TopNotificationProvider.track(false) {
            summaryView.updateAtAGlance()
        }
        NotificationService.MediaObserver.track {
            mediaView.update(it)
            summaryView.updateAtAGlance()
        }
        widgetView?.startListening()
        pinnedGrid.updateGridApps()
        Battery.PowerSaver.track {
            if (it != sheetCallback is MinimalDrawerSheetCallback)
                onPowerSaverModeChanged(it)
        }
        TaskRunner.submit {
            drawerArea.onAppsChanged()
        }
        homeScreen.post {
            // Just in case it died for some reason (in post cause lower priority)
            if (NotificationService.hasPermission(this))
                startService(Intent(this, NotificationService::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        AppLoader.release()
        NotificationService.MediaObserver.release()
        TopNotificationProvider.release()
        Battery.release()
        Battery.PowerSaver.release()
        mediaView.clearData()
        widgetView?.stopListening()
        SuggestionsManager.saveToStorage(this)
        summaryView.clearData()
    }

    override fun onResume() {
        super.onResume()
        TaskRunner.submit {
            val events = EventsLoader.load(this)
            summaryView.updateEvents(events)
        }
    }

    private fun onPowerSaverModeChanged(isOn: Boolean) {
        sheetBehavior.removeBottomSheetCallback(sheetCallback)
        if (sheetBehavior.state != STATE_COLLAPSED)
            sheetCallback.onSlide(sheet, 0f)
        sheetCallback = if (isOn) MinimalDrawerSheetCallback()
            else DrawerSheetCallback()
        if (sheetBehavior.state != STATE_COLLAPSED)
            sheetCallback.onSlide(sheet, 1f)
        sheetBehavior.addBottomSheetCallback(sheetCallback)
    }

    private fun showDropTargets() {
        // Otherwise the bottom sheet behavior gets confused, idk why
        drawerArea.recyclerView.requestDisallowInterceptTouchEvent(true)
        sheetBehavior.state = STATE_COLLAPSED
        pinnedGrid.showDropTargets = true
    }

    private fun onDrawerItemOpened(item: LauncherItem) {
        sheetBehavior.state = STATE_COLLAPSED
    }

    private fun search() {
        sheetBehavior.state = STATE_EXPANDED
        drawerArea.focusSearch()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sheetBehavior.state != STATE_COLLAPSED)
            sheetBehavior.state = STATE_COLLAPSED
    }

    override fun onNewIntent(intent: Intent) {
        val alreadyOnHome = (hasWindowFocus() || LongPressMenu.isInFocus()) && ((intent.flags and
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)

        when (intent.action) {
            Intent.ACTION_MAIN -> {
                if (alreadyOnHome) {
                    if (sheetBehavior.state == STATE_EXPANDED) {
                        LongPressMenu.dismissCurrent()
                        sheetBehavior.state = STATE_COLLAPSED
                    } else if (sheetBehavior.state == STATE_COLLAPSED) {
                        if (!LongPressMenu.dismissCurrent())
                            sheetBehavior.state = STATE_EXPANDED
                    }
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    handleGestureContract(intent)
            }
            Intent.ACTION_ALL_APPS -> sheetBehavior.state = STATE_EXPANDED
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleGestureContract(intent: Intent) {
        val gnc = GestureNavContract.fromIntent(intent) ?: return
        val app = App(gnc.componentName.packageName, gnc.componentName.className, gnc.user)
        val bounds = pinnedGrid.hideIconAndGetBounds(app) ?: return

        bounds.offset(-drawCtx.iconSize / 2, -drawCtx.iconSize / 2)

        val icon = IconLoader.loadIcon(this, app)

        val s = (drawCtx.iconSize * 2).toInt()
        val picture = Picture().record(s, s) {
            icon.setBounds(width / 4, height / 4, width / 4 * 3, height / 4 * 3)
            icon.draw(this)
        }

        val surfaceView = FloatingIconView(this, gnc, bounds, picture) {
            pinnedGrid.unhideIcon()
        }
        homeScreen.addView(surfaceView, LayoutParams(s, s))
        surfaceView.bringToFront()
    }

    private fun applyCustomizations() {
        val settings = ionApplication.settings
        screenBackgroundColor = ColorThemer.wallBackground(this)
        drawerBackgroundColor = ColorThemer.drawerBackground(this)
        screenBackground.color = screenBackgroundColor
        val dp = resources.displayMetrics.density
        Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.wallForeground(this)) < 0.5f)
        drawCtx.applyCustomizations(this, settings)
        pinnedGrid.applyCustomizations(settings)
        drawerArea.applyCustomizations()
        mediaView.applyCustomizations(settings)
        summaryView.applyCustomizations(settings)
        suggestionsView.applyCustomizations(settings)
        val m = pinnedGrid.calculateSideMargin()
        summaryView.setPadding(m, m.coerceAtLeast(Utils.getStatusBarHeight(this) + m / 2), m, m)
        mediaView.setPadding(m, m / 2, m, m / 2)
        suggestionsView.setPadding(m, m / 2, m, m / 2)
        suggestionsView.updateLayoutParams {
            height = (settings["dock:icon-size", 48] * dp).toInt() + m
        }
        val b = (Utils.getNavigationBarHeight(this@HomeScreen) - m / 2)
            .coerceAtLeast(m / 2)
        desktop.setPadding(0, 0, 0, b)
        val widget = Widgets.getWidget(this)
        if (widget != widgetView?.widget) {
            if (widgetView != null) {
                desktop.removeView(widgetView)
                widgetView = null
            }
            if (widget != null) {
                widgetView = WidgetView.new(this, widget)
                desktop.addView(widgetView, 2, LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    (96 * dp).toInt(),
                ).apply {
                    gravity = Gravity.CENTER
                    val v = (dp * 6).toInt()
                    setMargins(m, 0, m, v)
                })
            }
        }
    }
}