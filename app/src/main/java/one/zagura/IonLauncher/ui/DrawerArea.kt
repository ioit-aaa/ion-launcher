package one.zagura.IonLauncher.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.search.AppsProvider
import one.zagura.IonLauncher.provider.search.ContactsProvider
import one.zagura.IonLauncher.provider.search.Search
import one.zagura.IonLauncher.provider.search.SettingsProvider
import one.zagura.IonLauncher.provider.search.ShortcutsProvider
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.Utils

@SuppressLint("UseCompatLoadingForDrawables", "ViewConstructor")
class DrawerArea(
    context: Activity,
    showDropTargets: () -> Unit,
    onItemOpened: (LauncherItem) -> Unit,
) : LinearLayout(context) {

    val recyclerView: RecyclerView
    private val searchAdapter = SearchAdapter(showDropTargets, onItemOpened, context)
    private val entry: EditText
    private val separator: View
    private val icSearch = context.getDrawable(R.drawable.ic_search)!!

    private var results = emptyList<LauncherItem>()
    private var isDrawer = true

    init {
        val dp = context.resources.displayMetrics.density
        entry = EditText(context).apply {
            background = null
            isSingleLine = true
            typeface = Typeface.DEFAULT_BOLD
            setCompoundDrawablesRelativeWithIntrinsicBounds(icSearch, null, null, null)
            compoundDrawablePadding = (12 * dp).toInt()
            val h = (24 * dp).toInt()
            val v = (10 * dp).toInt()
            setPadding(h, v, h, v)
            includeFontPadding = false
            setHint(R.string.search)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    search(s.toString())
                }
            })
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    results.firstOrNull()?.open(v)
                    true
                } else false
            }
            imeOptions = EditorInfo.IME_ACTION_GO
        }
        separator = View(context)
        recyclerView = RecyclerView(context).apply {
            adapter = searchAdapter
            clipToPadding = false
            setHasFixedSize(true)
            itemAnimator = null
            val p = (12 * dp).toInt()
            setPadding(p, p, p, p.coerceAtLeast(Utils.getNavigationBarHeight(context)))
            layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(i: Int) =
                        if (isDrawer) 1 else 2
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
        run {
            orientation = VERTICAL
            isInvisible = true
            alpha = 0f
            addView(entry, MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = Utils.getStatusBarHeight(context)
            })
            addView(separator, LayoutParams(MATCH_PARENT, dp.toInt()))
            addView(recyclerView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    fun clearSearch() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        entry.text.clear()
        unsearch()
        entry.clearFocus()
        recyclerView.stopScroll()
        recyclerView.scrollToPosition(0)
    }

    fun onAppsChanged() {
        if (isDrawer)
            searchAdapter.notifyDataSetChanged()
    }

    private fun unsearch() {
        results = emptyList()
        isDrawer = true
        val apps = AppLoader.getResource()
        searchAdapter.update(apps)
        notSearchedYet = true
    }

    private var notSearchedYet = true

    private fun search(query: String) {
        if (query.isBlank()) {
            unsearch()
            return
        }
        if (notSearchedYet) {
            notSearchedYet = false
            reloadProviders()
        }
        isDrawer = false
        results = Search.query(query)
        searchAdapter.update(results)
    }

    private fun reloadProviders() {
        Search.updateData(context)
        search(entry.text.toString())
    }

    fun applyCustomizations() {
        searchAdapter.notifyDataSetChanged()
        if (isDrawer)
            unsearch()
        val fgColor = ColorThemer.foreground(context)
        val hintColor = ColorThemer.hint(context)
        with(entry) {
            setTextColor(fgColor)
            setHintTextColor(hintColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                textCursorDrawable?.setTint(fgColor)
            highlightColor = fgColor and 0xffffff or 0x33000000
        }
        icSearch.setTint(hintColor)
        separator.background = FillDrawable(hintColor)
    }
}