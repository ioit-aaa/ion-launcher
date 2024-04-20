package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.luminance
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Widgets
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.drawer.DrawerArea
import one.zagura.IonLauncher.ui.view.MusicView
import one.zagura.IonLauncher.ui.view.PinnedGridView
import one.zagura.IonLauncher.ui.view.SuggestionsView
import one.zagura.IonLauncher.ui.view.SummaryView
import one.zagura.IonLauncher.ui.view.WidgetView
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.Utils

class HomeScreen : Activity() {

    companion object {
        const val SEARCH_ICON_SIZE = 32
    }

    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var sheet: View

    private lateinit var homeScreen: CoordinatorLayout
    private lateinit var desktop: LinearLayout
    private lateinit var drawerArea: DrawerArea

    private lateinit var summaryView: SummaryView
    private lateinit var musicView: MusicView
    private var widgetView: WidgetView? = null
    private lateinit var pinnedGrid: PinnedGridView
    private lateinit var suggestionsView: SuggestionsView

    private val screenBackground = FillDrawable(0)
    private var screenBackgroundAlpha = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    override fun onStart() {
        super.onStart()
        ionApplication.settings.consumeUpdate {
            IconLoader.updateIconPacks(this, ionApplication.settings)
            applyCustomizations()
        }
        AppLoader.track(false) {
            runOnUiThread {
                drawerArea.onAppsChanged()
                pinnedGrid.updateGridApps()
            }
        }
        NotificationService.MediaObserver.track {
            musicView.updateTrack(it)
        }
        widgetView?.startListening()
        suggestionsView.update()
        pinnedGrid.updateGridApps()
        homeScreen.post {
            drawerArea.onAppsChanged()
            // windowToken might not be loaded, so we post this to the view
            val wallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            wallpaperManager.setWallpaperOffsets(homeScreen.windowToken, 0.5f, 0.5f)
            // Just in case it died for some reason (in post cause lower priority)
            if (NotificationService.hasPermission(this))
                startService(Intent(this, NotificationService::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        AppLoader.release()
        NotificationService.MediaObserver.release()
        widgetView?.stopListening()
        SuggestionsManager.saveToStorage(this)
    }

    override fun onResume() {
        super.onResume()
        ionApplication.task {
            val events = EventsLoader.load(this)
            runOnUiThread {
                summaryView.updateEvents(events)
            }
        }
    }

    private fun applyCustomizations() {
        val settings = ionApplication.settings
        screenBackground.color = ColorThemer.background(this)
        screenBackgroundAlpha = settings["color:bg:alpha", 0xdd]
        screenBackground.alpha = screenBackgroundAlpha
        val dp = resources.displayMetrics.density
        Utils.setDarkStatusFG(window, ColorThemer.foreground(this).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                it.luminance < 0.5f
            else ColorThemer.foreground(this).let(ColorThemer::lightness) < 0.5f
        })
        pinnedGrid.applyCustomizations()
        drawerArea.applyCustomizations()
        musicView.applyCustomizations()
        summaryView.applyCustomizations()
        val m = pinnedGrid.calculateSideMargin()
        summaryView.setPadding(m, m.coerceAtLeast(Utils.getStatusBarHeight(this) + m / 2), m, m)
        musicView.setPadding(m, 0, m, m - (8 * dp).toInt())
        val v = (dp * 6).toInt()
        suggestionsView.setPadding(m, v, m, v)
        suggestionsView.updateLayoutParams {
            height = (settings["dock:icon-size", 48] * dp).toInt() + v * 2
        }
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
                    setMargins(m, 0, m, v)
                })
            }
        }
    }

    private fun createHomeScreen(): CoordinatorLayout {
        val dp = resources.displayMetrics.density
        val fullHeight = Utils.getDisplayHeight(this)

        summaryView = SummaryView(this)
        musicView = MusicView(this)
        suggestionsView = SuggestionsView(this, ::showDropTargets)
        pinnedGrid = PinnedGridView(this)

        desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(
                summaryView,
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            addView(
                musicView,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            addView(pinnedGrid,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, pinnedGrid.calculateGridHeight()))
            addView(
                suggestionsView,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, 0))
            setPadding(0, 0, 0, Utils.getNavigationBarHeight(this@HomeScreen))
        }

        val offset = (256 * dp).toInt()
        drawerArea = DrawerArea(this, ::showDropTargets, ::onDrawerItemOpened).apply {
            setPadding(0, offset, 0, 0)
        }

        sheet = FrameLayout(this).apply {
            addView(desktop, LayoutParams(LayoutParams.MATCH_PARENT, fullHeight))
            addView(drawerArea, LayoutParams(LayoutParams.MATCH_PARENT, fullHeight + offset))
        }

        return CoordinatorLayout(this).apply {
            fitsSystemWindows = false
            addView(sheet, CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, fullHeight + offset).apply {
                sheetBehavior = BottomSheetBehavior<View>().apply {
                    isHideable = false
                    peekHeight = fullHeight
                    state = STATE_COLLAPSED
                    addBottomSheetCallback(object : BottomSheetCallback() {
                        override fun onStateChanged(view: View, newState: Int) {
                            if (newState != STATE_EXPANDED)
                                drawerArea.clearSearch()
                            desktop.isVisible = newState != STATE_EXPANDED
                            if (newState == STATE_COLLAPSED) {
                                drawerArea.isInvisible = true
                                desktop.bringToFront()
                            } else {
                                drawerArea.isInvisible = false
                                drawerArea.bringToFront()
                            }
                        }

                        override fun onSlide(view: View, slideOffset: Float) {
                            drawerArea.alpha = slideOffset * slideOffset / 0.6f - 0.4f
                            val a = (slideOffset * 2f).coerceAtMost(1f)
                            screenBackground.alpha = (screenBackgroundAlpha * (1f - a) + 255 * a).toInt()
                            desktop.alpha = 1f - a
                            desktop.translationY = slideOffset * offset * 0.75f
                            val scale = 1f - slideOffset * 0.02f
                            desktop.scaleX = scale
                            desktop.scaleY = scale
                        }
                    })
                }
                behavior = sheetBehavior
            })
        }
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sheetBehavior.state != STATE_COLLAPSED)
            sheetBehavior.state = STATE_COLLAPSED
    }
}