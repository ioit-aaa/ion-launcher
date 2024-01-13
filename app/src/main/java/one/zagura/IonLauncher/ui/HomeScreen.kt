package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowInsetsController
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
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.view.ItemGridView
import one.zagura.IonLauncher.ui.view.MusicView
import one.zagura.IonLauncher.ui.view.SuggestionsView
import one.zagura.IonLauncher.ui.view.SummaryView
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
    private lateinit var suggestionsView: SuggestionsView
    private lateinit var pinnedGrid: ItemGridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        homeScreen = createHomeScreen()
        val h = Utils.getDisplayHeight(this) * 2
        setContentView(homeScreen, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, h, Gravity.BOTTOM))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
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

        IconLoader.updateIconPacks(this, ionApplication.settings)
        applyCustomizations()
    }

    override fun onResume() {
        super.onResume()
        ionApplication.settings.consumeUpdate {
            IconLoader.updateIconPacks(this, ionApplication.settings)
            applyCustomizations()
        }
        suggestionsView.update()
        summaryView.updateEvents(EventsLoader.load(this))
        AppLoader.track {
            pinnedGrid.updateGridApps()
        }
        NotificationService.MediaObserver.track {
            musicView.updateTrack(it)
        }
        homeScreen.post {
            // windowToken might not be loaded, so we post this to the view
            val wallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            wallpaperManager.setWallpaperOffsets(homeScreen.windowToken, 0.5f, 0.5f)
        }
    }

    override fun onPause() {
        super.onPause()
        AppLoader.release()
        NotificationService.MediaObserver.release()
        SuggestionsManager.saveToStorage(this)
    }

    @Suppress("DEPRECATION")
    private fun applyCustomizations() {
        val dp = resources.displayMetrics.density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val f = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (ColorThemer.foreground(this).luminance > 0.5f) 0 else f, f)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = if (ColorThemer.foreground(this).luminance > 0.5f) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (ColorThemer.foreground(this).let(ColorThemer::lightness) > 0.5f) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        homeScreen.background = FillDrawable(ColorThemer.backgroundOverlay(this))
        pinnedGrid.applyCustomizations()
        drawerArea.applyCustomizations()
        musicView.applyCustomizations()
        summaryView.applyCustomizations()
        val m = pinnedGrid.calculateSideMargin()
        summaryView.setPadding(m, m.coerceAtLeast(Utils.getStatusBarHeight(this) + m / 2), m, m)
        musicView.setPadding(m, 0, m, m - (8 * dp).toInt())
        suggestionsView.updateLayoutParams<MarginLayoutParams> {
            val v = (dp * 6).toInt()
            setMargins(m, v, m, v)
        }
    }

    private fun createHomeScreen(): CoordinatorLayout {
        val dp = resources.displayMetrics.density
        val fullHeight = Utils.getDisplayHeight(this)

        summaryView = SummaryView(this)
        musicView = MusicView(this)
        suggestionsView = SuggestionsView(this, ::showDropTargets)
        pinnedGrid = ItemGridView(this)

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
                MarginLayoutParams(LayoutParams.MATCH_PARENT, (32 * dp).toInt()))
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
                            val inv = 1f - slideOffset
                            drawerArea.alpha = 1f - (inv * inv)
                            desktop.translationY = slideOffset * offset / 2
                            desktop.alpha = inv
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