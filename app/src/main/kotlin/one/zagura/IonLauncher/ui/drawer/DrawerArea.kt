package one.zagura.IonLauncher.ui.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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
import one.zagura.IonLauncher.ui.view.CategoryBoxView
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

    val libraryView: RecyclerView
    val recyclerView: RecyclerView

    private var categorize = true
    private var results = emptyList<LauncherItem>()

    enum class Screen {
        Library, Category, Search
    }
    private var screen: Screen = Screen.Library
        set(s) {
            field = s
            when (s) {
                Screen.Library -> {
                    libraryView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
//                    extraButton.setImageResource(R.drawable.options)
                }
                Screen.Category -> {
                    recyclerView.visibility = View.VISIBLE
                    libraryView.visibility = View.GONE
//                    extraButton.setImageResource(R.drawable.options)
                    recyclerView.adapter = categoryAdapter
                }
                Screen.Search -> {
                    recyclerView.visibility = View.VISIBLE
                    libraryView.visibility = View.GONE
//                    extraButton.setImageResource(R.drawable.cross)
                    recyclerView.adapter = searchAdapter
                }
            }
        }

    init {
        val dp = resources.displayMetrics.density
        libraryView = RecyclerView(context).apply {
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
        recyclerView = RecyclerView(context).apply {
            visibility = View.GONE
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
        }
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

    private var notSearchedYet = true
    private var searchCancellable = Cancellable()

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
    
    private fun openCategory(category: AppCategorizer.AppCategory, apps: List<App>) {
        screen = Screen.Category
        categoryAdapter.update(category, apps)
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

    fun openAllApps() = openCategory(AppCategorizer.AppCategory.AllApps, AppLoader.getResource())
}