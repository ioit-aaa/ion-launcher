package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Picture
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.record
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Widgets
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.items.AppCategorizer
import one.zagura.IonLauncher.provider.notification.NotificationService
import one.zagura.IonLauncher.provider.notification.TopNotificationProvider
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.provider.summary.EventsLoader
import one.zagura.IonLauncher.ui.drawer.DrawerArea
import one.zagura.IonLauncher.ui.smartspacer.CustomSmartspaceView
import one.zagura.IonLauncher.ui.view.Gestures
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.ui.view.MediaView
import one.zagura.IonLauncher.ui.view.PinnedGridView
import one.zagura.IonLauncher.ui.view.SharedDrawingContext
import one.zagura.IonLauncher.ui.view.SuggestionRowView
import one.zagura.IonLauncher.ui.view.SummaryView
import one.zagura.IonLauncher.ui.view.WidgetView
import one.zagura.IonLauncher.util.iconify.FloatingIconView
import one.zagura.IonLauncher.util.iconify.GestureNavContract
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
    private lateinit var smartspacerView: CustomSmartspaceView

    private lateinit var mediaView: MediaView

    private var widgetView: WidgetView? = null

    private lateinit var pinnedGrid: PinnedGridView
    private lateinit var bottomBar: FrameLayout
    private lateinit var suggestionsView: SuggestionRowView
    private lateinit var searchEntry: EditText

    private lateinit var drawCtx: SharedDrawingContext

    private val screenBackground = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf())
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
        homeScreen.background = screenBackground

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
                onBackPressed()
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val w = getSystemService(WALLPAPER_SERVICE) as WallpaperManager
            w.addOnColorsChangedListener(object : WallpaperManager.OnColorsChangedListener {
                override fun onColorsChanged(
                    colors: WallpaperColors?,
                    which: Int
                ) {
                    if ((which and WallpaperManager.FLAG_SYSTEM) == 0)
                        return
                    IconLoader.updateIconPacks(this@HomeScreen, ionApplication.settings)
                    applyCustomizations(false)
                }

            }, desktop.handler)
        }
        IconLoader.updateIconPacks(this, ionApplication.settings)
        applyCustomizations(true)
        SuggestionsManager.track {
            suggestionsView.update(it)
        }
        homeScreen.post {
            // windowToken might not be loaded, so we post this to the view
            val wallpaperManager = getSystemService(WALLPAPER_SERVICE) as WallpaperManager
            wallpaperManager.setWallpaperOffsetSteps(0f, 0f)
            wallpaperManager.setWallpaperOffsets(homeScreen.windowToken, 0.5f, 0.5f)
        }
    }

    private fun createHomeScreen(): CoordinatorLayout {
        val dp = resources.displayMetrics.density
        val fullHeight = Utils.getDisplayHeight(this)

        summaryView = SummaryView(this, drawCtx)
        mediaView = MediaView(this, drawCtx)
        pinnedGrid = PinnedGridView(this, drawCtx)

        suggestionsView = SuggestionRowView(this, drawCtx, ::showDropTargets, ::search) {
            val dp = resources.displayMetrics.density
            if (it.showCross)
                drawerArea.clearSearchField()
            else
                LongPressMenu.popupLauncher(it,
                    Gravity.BOTTOM or Gravity.END, (12 * dp).toInt(),
                    it.height + (12 * dp).toInt())
        }
        searchEntry = EditText(this).apply {
            background = null
            isSingleLine = true
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setHint(R.string.search)
            doOnTextChanged { s, _, _, _ ->
                drawerArea.onSearchTextChanged(s)
                suggestionsView.showCross = !s.isNullOrEmpty()
            }
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    drawerArea.onTextGoAction(v)
                    true
                } else false
            }
            imeOptions = EditorInfo.IME_ACTION_GO
            alpha = 0f
            isVisible = false
        }

        smartspacerView = CustomSmartspaceView(this)

        desktop = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                addView(
                    smartspacerView,
                    LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, .5f))
            addView(
                summaryView,
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            addView(
                mediaView,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            addView(pinnedGrid,
                MarginLayoutParams(LayoutParams.MATCH_PARENT, pinnedGrid.calculateGridHeight()))
        }

        val offset = (256 * dp).toInt()
        drawerArea = DrawerArea(this, drawCtx, ::showDropTargets, ::onDrawerItemOpened, searchEntry).apply {
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
            addBottomSheetCallback(sheetCallback)
        }

        bottomBar = FrameLayout(this).apply {
            addView(suggestionsView, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(searchEntry, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            setOnApplyWindowInsetsListener { v, insets ->
                val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemBars()).bottom
                else
                    insets.systemWindowInsetBottom
                v.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = b
                }
                insets
            }
        }

        return CoordinatorLayout(this).apply {
            fitsSystemWindows = false
            addView(sheet, CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, fullHeight + offset).apply {
                behavior = sheetBehavior
            })
            addView(
                bottomBar,
                CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
                    gravity = Gravity.BOTTOM
                })
        }
    }

    inner class DrawerSheetCallback : BottomSheetCallback() {
        private val wallpaperManager = getSystemService(WALLPAPER_SERVICE) as WallpaperManager
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
            updateBGColors(Utils.getDisplayHeight(this@HomeScreen) - pinnedGrid.y.toFloat(), a)
            desktop.alpha = inva
            desktop.translationY = a * offset
            val scale = 1f - a * 0.05f
            desktop.scaleX = scale
            desktop.scaleY = scale
            val token = homeScreen.windowToken
            if (token != null)
                setWallpaperZoomOut?.invoke(wallpaperManager, token, slideOffset)
            searchEntry.alpha = (slideOffset - 0.5f).coerceAtLeast(0f) * 2f
            suggestionsView.transitionToSearchBarHolder(slideOffset)
        }
    }

    inner class MinimalDrawerSheetCallback : BottomSheetCallback() {
        override fun onStateChanged(view: View, newState: Int) = onDrawerStateChanged(view, newState)
        override fun onSlide(view: View, slideOffset: Float) {
            val a = (slideOffset * 1.5f).coerceAtMost(1f)
            updateBGColors(Utils.getDisplayHeight(this@HomeScreen) - pinnedGrid.y.toFloat(), a)
            drawerArea.alpha = a * a * a
            desktop.alpha = 1f - a
            searchEntry.alpha = a
            suggestionsView.transitionToSearchBarHolder(slideOffset)
        }
    }

    fun onDrawerStateChanged(view: View, newState: Int) {
        if (newState == STATE_EXPANDED)
            Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.drawerForeground(this@HomeScreen)) < 0.5f)
        else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(drawerArea.windowToken, 0)
            drawerArea.clearSearchField()
            drawerArea.entry.clearFocus()
        }
        if (newState == STATE_EXPANDED && ionApplication.settings["drawer:auto_keyboard", false])
            drawerArea.focusSearch()
        desktop.isVisible = newState != STATE_EXPANDED
        if (newState == STATE_COLLAPSED) {
            searchEntry.isVisible = false
            drawerArea.isInvisible = true
            desktop.bringToFront()
            Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.wallForeground(this@HomeScreen)) < 0.5f)
        } else {
            searchEntry.isVisible = true
            drawerArea.isInvisible = false
            drawerArea.bringToFront()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SuggestionsManager.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            SmartspacerClient.close()
    }

    override fun onStart() {
        super.onStart()
        desktop.run {
            scaleX = 0.95f
            scaleY = 0.95f
            animate().scaleX(1f).scaleY(1f).setInterpolator {
                val ix = (1f - it)
                1f - ix * ix
            }.duration = 200L
        }
        ionApplication.settings.consumeUpdate {
            IconLoader.updateIconPacks(this, ionApplication.settings)
            suggestionsView.update(SuggestionsManager.getResource())
            NotificationService.MediaObserver.updateMediaItem(applicationContext)
            applyCustomizations(true)
        }
        AppCategorizer.track(false) { drawerArea.onAppsChanged(it) }
        AppLoader.track(false) { pinnedGrid.updateGridApps() }
        Battery.track(false) { summaryView.updateAtAGlance() }
        TopNotificationProvider.track(false) {
            summaryView.updateAtAGlance()
        }
        NotificationService.MediaObserver.track {
            mediaView.update(it)
            summaryView.updateAtAGlance()
        }
        widgetView?.startListening()
        Battery.PowerSaver.track {
            if (it != sheetCallback is MinimalDrawerSheetCallback)
                onPowerSaverModeChanged(it)
        }
        TaskRunner.submit {
            pinnedGrid.updateGridApps()
            drawerArea.onAppsChanged(AppCategorizer.getResource())
            // Just in case it died for some reason (in post cause lower priority)
            if (NotificationService.hasPermission(this))
                startService(Intent(this, NotificationService::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        sheetBehavior.state = STATE_COLLAPSED
        AppLoader.release()
        NotificationService.MediaObserver.release()
        TopNotificationProvider.release()
        Battery.release()
        Battery.PowerSaver.release()
        widgetView?.stopListening()
        TaskRunner.submit {
            summaryView.clearData()
            mediaView.clearData()
            SuggestionsManager.saveToStorage(this)
        }
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
        drawerArea.libraryView.requestDisallowInterceptTouchEvent(true)
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
            if (!drawerArea.onBackPressed())
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
                            Gestures.onHomePress(this)?.let { onNewIntent(intent.setAction(it)) }
                    }
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    handleGestureContract(intent)
            }
            Gestures.ACTION_OPEN_MENU_POPUP ->
                LongPressMenu.popupLauncher(homeScreen, Gravity.CENTER, 0, 0)
            Gestures.ACTION_OPEN_DRAWER -> sheetBehavior.state = STATE_EXPANDED
            Intent.ACTION_ALL_APPS -> {
                sheetBehavior.state = STATE_EXPANDED
                drawerArea.openAllApps()
            }
            Intent.ACTION_SEARCH -> {
                sheetBehavior.state = STATE_EXPANDED
                drawerArea.focusSearch()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleGestureContract(intent: Intent) {
        val gnc = GestureNavContract.fromIntent(intent) ?: return
        val packageName = gnc.componentName.packageName
        val user = gnc.user
        TaskRunner.submit {
            val (item, bounds, onAnimEnd) = pinnedGrid.prepareIconifyAnim(packageName, user)
                ?: suggestionsView.prepareIconifyAnim(packageName, user)
                ?: return@submit
            bounds.offset(-bounds.width() / 2, -bounds.height() / 2)
            val icon = IconLoader.loadIcon(this, item)
            icon.copyBounds(drawCtx.tmpRect)
            val s = (bounds.width() * 2).toInt()
            val picture = Picture().record(s, s) {
                icon.setBounds(width / 4, height / 4, width / 4 * 3, height / 4 * 3)
                icon.draw(this)
            }
            icon.bounds = drawCtx.tmpRect
            val surfaceView = FloatingIconView(this, gnc, bounds, picture, onAnimEnd)
            runOnUiThread {
                homeScreen.addView(surfaceView, LayoutParams(s, s))
                surfaceView.bringToFront()
            }
        }
    }

    private fun updateBGColors(bottomHeight: Float, a: Float) {
        val bottom = screenBackgroundColor and 0xffffff or (screenBackgroundColor.alpha.coerceAtLeast(240) shl 24)
        val c = ColorUtils.blendARGB(screenBackgroundColor, drawerBackgroundColor, a)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val h = Utils.getDisplayHeight(this)
            val top = screenBackgroundColor and 0xffffff or (screenBackgroundColor.alpha.coerceAtLeast(100) shl 24)
            screenBackground.setColors(intArrayOf(
                ColorUtils.blendARGB(top, drawerBackgroundColor, a),
                c, c,
                ColorUtils.blendARGB(bottom, drawerBackgroundColor, a)),
                floatArrayOf(
                    0.5f,
                    0.5f + Utils.getStatusBarHeight(this) / h.toFloat(),
                    0.5f + (h - bottomHeight * 1.15f) / h / 2f,
                    0.5f + (h - bottomHeight * 0.5f) / h / 2f))
        } else screenBackground.colors = intArrayOf(c, c, c,
            ColorUtils.blendARGB(bottom, drawerBackgroundColor, a))
    }

    private fun applyCustomizations(layout: Boolean) {
        val settings = ionApplication.settings
        val dp = resources.displayMetrics.density
        drawCtx.applyLayoutCustomizations(this, settings)
        drawCtx.applyColorCustomizations(this)
        if (layout)
            pinnedGrid.applyLayoutCustomizations(settings)
        val sideMargin = pinnedGrid.calculateSideMargin()
        drawerArea.applyCustomizationsColor(settings)
        mediaView.applyCustomizations(settings)
        summaryView.applyCustomizations(settings)
        suggestionsView.applyCustomizations(settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val replaceAtAGlance = settings["smartspacer:replace-ataglance", false]
            summaryView.showAtAGlance = !replaceAtAGlance
            if (replaceAtAGlance) {
                smartspacerView.isVisible = true
                smartspacerView.applyCustomizations(settings, sideMargin)
            } else
                smartspacerView.isVisible = false
        }

        Utils.setDarkStatusFG(window, ColorThemer.lightness(ColorThemer.wallForeground(this)) < 0.5f)
        screenBackgroundColor = ColorThemer.wallBackground(this)
        drawerBackgroundColor = ColorThemer.drawerBackground(this)

        val fgColor = ColorThemer.drawerForeground(this)
        val hintColor = ColorThemer.drawerHint(this)
        with(searchEntry) {
            setTextColor(fgColor)
            setHintTextColor(hintColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                textCursorDrawable?.setTint(fgColor)
            highlightColor = fgColor and 0xffffff or 0x33000000
        }

        if (layout) {
            summaryView.setPadding(sideMargin, sideMargin.coerceAtLeast(Utils.getStatusBarHeight(this) + sideMargin / 2), sideMargin, sideMargin)
            mediaView.setPadding(sideMargin, sideMargin / 2, sideMargin, sideMargin / 2)
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
            val bottomMargin = (sideMargin - Utils.getNavigationBarHeight(this@HomeScreen))
                .coerceAtLeast(0)
            bottomBar.updateLayoutParams<MarginLayoutParams> {
                height = iconSize + sideMargin / 2 + bottomMargin
            }

            suggestionsView.setPadding(sideMargin, sideMargin / 2, sideMargin, bottomMargin)
            with(searchEntry) {
                val v = (12 * dp).toInt()
                setPadding(0, sideMargin / 2 + v, 0, bottomMargin + v)
                updateLayoutParams<MarginLayoutParams> {
                    leftMargin = iconSize + sideMargin
                    rightMargin = iconSize + sideMargin
                }
            }
            val b = (Utils.getNavigationBarHeight(this@HomeScreen))
                .coerceAtLeast(sideMargin) - sideMargin / 2
            val bottomBarSpaceHeight = iconSize + sideMargin + b
            desktop.setPadding(0, 0, 0, bottomBarSpaceHeight)
            drawerArea.applyCustomizationsLayout(settings, sideMargin, bottomBarSpaceHeight)
            val widget = Widgets.getWidget(this)
            if (widget != widgetView?.widget) {
                if (widgetView != null) {
                    desktop.removeView(widgetView)
                    widgetView = null
                }
                if (widget != null) {
                    widgetView = WidgetView.new(this, widget)
                    desktop.addView(
                        widgetView, 2, LinearLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            (96 * dp).toInt(),
                        ).apply {
                            gravity = Gravity.CENTER
                            val v = (dp * 6).toInt()
                            setMargins(sideMargin, 0, sideMargin, v)
                        })
                }
            }
            updateBGColors(pinnedGrid.calculateGridHeight() + bottomBarSpaceHeight.toFloat(), 0f)
        } else
            updateBGColors(Utils.getDisplayHeight(this@HomeScreen) - pinnedGrid.y.toFloat(), 0f)
    }
}