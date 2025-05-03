package one.zagura.IonLauncher.ui.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.AppCategorizer
import one.zagura.IonLauncher.provider.search.Search
import one.zagura.IonLauncher.provider.summary.Battery
import one.zagura.IonLauncher.ui.view.CategoryBoxView
import one.zagura.IonLauncher.ui.view.ClickableRecyclerView
import one.zagura.IonLauncher.ui.view.SharedDrawingContext
import one.zagura.IonLauncher.util.Cancellable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils

@SuppressLint("UseCompatLoadingForDrawables", "ViewConstructor")
class DrawerArea(
    context: Activity,
    drawCtx: SharedDrawingContext,
    showDropTargets: () -> Unit,
    onItemOpened: (LauncherItem) -> Unit,
    val entry: EditText,
) : FrameLayout(context) {

    private val libraryAdapter = LibraryAdapter(showDropTargets, onItemOpened, context, drawCtx, ::openCategory)
    private val categoryAdapter = CategoryAdapter(showDropTargets, onItemOpened, context)
    private val searchAdapter = SearchAdapter(showDropTargets, onItemOpened, context)

    private var categorize = true
    private var results = emptyList<LauncherItem>()

    private var notSearchedYet = true
    private var searchCancellable = Cancellable()

    val libraryView = RecyclerView(context).apply {
        adapter = libraryAdapter
        clipToPadding = false
        setHasFixedSize(true)
        itemAnimator = null
        layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(v: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && entry.isFocused) {
                    entry.clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        })
    }
    val recyclerView = ClickableRecyclerView(context).apply {
        visibility = GONE
        adapter = searchAdapter
        clipToPadding = false
        setHasFixedSize(true)
        itemAnimator = null
        layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(i: Int) =
                    if (screen == Screen.Search || i == 0) spanCount else 1
            }
        }
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(v: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && entry.isFocused) {
                    entry.clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        })
        setOnClickListener {
            screen = Screen.Library
        }
    }

    enum class Screen {
        Library, Category, Search
    }
    private var screen: Screen = Screen.Library
        set(s) {
            val prev = field
            field = s
            when (s) {
                Screen.Library -> {
                    if (Battery.PowerSaver.getResource()) {
                        libraryView.visibility = VISIBLE
                        libraryView.alpha = 1f
                        recyclerView.visibility = GONE
                        return
                    }
                    libraryView.bringToFront()
                    libraryView.visibility = VISIBLE
                    libraryView.animate().alpha(1f).duration = 100L
                    recyclerView.animate().alpha(0f).withEndAction {
                        recyclerView.visibility = GONE
                    }.setInterpolator(AccelerateInterpolator()).duration = 150L
                    if (prev == Screen.Category)
                        categoryAdapter.animateExitTransition(recyclerView)
                }
                Screen.Category -> {
                    if (Battery.PowerSaver.getResource()) {
                        recyclerView.visibility = VISIBLE
                        recyclerView.alpha = 1f
                        libraryView.visibility = GONE
                        return
                    }
                    recyclerView.bringToFront()
                    recyclerView.visibility = VISIBLE
                    recyclerView.animate().alpha(1f).setInterpolator(DecelerateInterpolator()).duration = 150L
                    libraryView.animate().alpha(0f).withEndAction {
                        libraryView.visibility = GONE
                    }.setInterpolator(AccelerateInterpolator()).duration = 100L
                    recyclerView.adapter = categoryAdapter
                }
                Screen.Search -> {
                    recyclerView.bringToFront()
                    libraryView.visibility = GONE
                    recyclerView.visibility = VISIBLE
                    recyclerView.alpha = 1f
                    recyclerView.adapter = searchAdapter
                }
            }
        }


    init {
        isInvisible = true
        alpha = 0f
        addView(libraryView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(recyclerView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    fun onSearchTextChanged(s: CharSequence?) {
        searchCancellable.cancel()
        TaskRunner.submit {
            search(s?.toString() ?: "")
        }
    }
    fun onTextGoAction(view: TextView) {
        results.firstOrNull()?.open(view)
    }

    fun focusSearch() {
        entry.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(entry, 0)
    }

    fun clearSearchField() {
        entry.text.clear()
        unsearch()
        recyclerView.stopScroll()
        recyclerView.scrollToPosition(0)
    }

    fun onAppsChanged(categories: Map<AppCategorizer.AppCategory, List<App>>) {
        when (screen) {
            Screen.Search -> {
                searchCancellable.cancel()
                Search.updateData(context)
                search(entry.text.toString())
            }
            Screen.Library -> post {
                libraryAdapter.update(
                    listOf(AppCategorizer.AppCategory.AllApps to AppLoader.getResource()) +
                        categories.toList().sortedBy {
                            CategoryBoxView.getNameForCategory(context, it.first)
                        })
            }
            Screen.Category -> post {
                categoryAdapter.update(
                    categoryAdapter.category,
                    if (categoryAdapter.category == AppCategorizer.AppCategory.AllApps)
                        AppLoader.getResource()
                    else categories[categoryAdapter.category] ?: emptyList()
                )
            }
        }
    }

    private fun unsearch() {
        results = emptyList()
        screen = if (categorize) Screen.Library else Screen.Category
        notSearchedYet = true
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            post { unsearch() }
            return
        }
        if (notSearchedYet) {
            post {
                screen = Screen.Search
            }
            notSearchedYet = false
            Search.updateData(context)
        }
        searchCancellable = Cancellable()
        results = Search.query(query, searchCancellable)
        post {
            searchAdapter.update(results)
        }
    }
    
    private fun openCategory(category: AppCategorizer.AppCategory, apps: List<App>, view: CategoryBoxView) {
        screen = Screen.Category
        categoryAdapter.update(category, apps)
        if (Battery.PowerSaver.getResource()) {
            recyclerView.scaleX = 1f
            recyclerView.scaleY = 1f
            return
        }
        categoryAdapter.animateEnterTransition(view, recyclerView)
        val l = IntArray(2).apply(view::getLocationOnScreen)
        view.animate()
            .scaleX(2f).scaleY(2f)
            .translationX(resources.displayMetrics.widthPixels / 2f - (l[0] + view.width / 2))
            .translationY(resources.displayMetrics.heightPixels / 2f - (l[1] + view.height / 2))
            .setInterpolator { it * it }
            .withEndAction {
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationX = 0f
                view.translationY = 0f
            }
            .duration = 120L
    }

    fun applyCustomizationsColor(settings: Settings) {
        searchAdapter.notifyDataSetChanged()
        categorize = settings["drawer:categories", true]
        categoryAdapter.update(AppCategorizer.AppCategory.AllApps, AppLoader.getResource())
        if (screen != Screen.Search)
            unsearch()
        categoryAdapter.showLabels = settings["drawer:labels", true]
    }

    fun applyCustomizationsLayout(settings: Settings, sideMargin: Int, bottomPadding: Int) {
        with(recyclerView) {
            (layoutManager as GridLayoutManager).spanCount = settings["dock:columns", 5]
            adapter = adapter
            val p = sideMargin / 2
            setPadding(p, p.coerceAtLeast(Utils.getStatusBarHeight(context)), p, p + bottomPadding)
        }
        with(libraryView) {
            adapter = adapter
            val p = sideMargin / 2
            setPadding(p, p.coerceAtLeast(Utils.getStatusBarHeight(context)), p, p + bottomPadding)
        }
    }

    fun onBackPressed(): Boolean {
        if (categorize && screen == Screen.Category) {
            screen = Screen.Library
            return true
        }
        return false
    }

    fun openAllApps() {
        screen = Screen.Category
        categoryAdapter.update(AppCategorizer.AppCategory.AllApps, AppLoader.getResource())
    }
}