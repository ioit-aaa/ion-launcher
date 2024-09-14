package one.zagura.IonLauncher.ui.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.search.Search
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.util.Cancellable
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.Utils

@SuppressLint("UseCompatLoadingForDrawables", "ViewConstructor")
class DrawerArea(
    context: Activity,
    showDropTargets: () -> Unit,
    onItemOpened: (LauncherItem) -> Unit,
) : LinearLayout(context) {

    val recyclerView: RecyclerView
    private val searchAdapter = SearchAdapter(showDropTargets, onItemOpened, context)
    val entry: EditText
    private val extraButton: ImageView
    private val bottomBar: LinearLayout
    private val separator: View
    private val icSearch = context.getDrawable(R.drawable.search)!!

    private var results = emptyList<LauncherItem>()

    private val layout = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(i: Int) =
                if (searchAdapter.isSearch || i == 0) spanCount else 1
        }
    }

    init {
        val dp = resources.displayMetrics.density
        entry = EditText(context).apply {
            background = null
            isSingleLine = true
            typeface = Typeface.DEFAULT_BOLD
            setCompoundDrawablesRelativeWithIntrinsicBounds(icSearch, null, null, null)
            compoundDrawablePadding = (8 * dp).toInt()
            includeFontPadding = false
            setHint(R.string.search)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    searchCancellable.cancel()
                    TaskRunner.submit {
                        search(s.toString())
                    }
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
        extraButton = ImageView(context).apply {
            setImageResource(R.drawable.cross)
            setOnClickListener {
                if (searchAdapter.isSearch)
                    clearSearchField()
                else {
                    LongPressMenu.popupLauncher(it,
                        Gravity.BOTTOM or Gravity.END, 0,
                        it.height + Utils.getNavigationBarHeight(context))
                }
            }
        }
        separator = View(context)
        recyclerView = RecyclerView(context).apply {
            adapter = searchAdapter
            clipToPadding = false
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = layout
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
        orientation = VERTICAL
        isInvisible = true
        alpha = 0f
        addView(recyclerView, LayoutParams(MATCH_PARENT, 0, 1f))
        addView(separator, LayoutParams(MATCH_PARENT, dp.toInt()))
        addView(LinearLayout(context).apply {
            bottomBar = this
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(entry, LayoutParams(0, WRAP_CONTENT, 1f))
            addView(extraButton, LayoutParams((40 * dp).toInt(), MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemBars()).bottom
                else
                    insets.systemWindowInsetBottom
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, b)
                insets
            }
        })
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

    fun onAppsChanged() {
        if (searchAdapter.isSearch) {
            searchCancellable.cancel()
            Search.updateData(context)
            search(entry.text.toString())
        } else post {
            searchAdapter.update(AppLoader.getResource(), false)
        }
    }

    private fun unsearch() {
        results = emptyList()
        val apps = AppLoader.getResource()
        searchAdapter.update(apps, false)
        extraButton.setImageResource(R.drawable.options)
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
            notSearchedYet = false
            Search.updateData(context)
        }
        searchCancellable = Cancellable()
        results = Search.query(query, searchCancellable)
        post {
            searchAdapter.update(results, true)
            extraButton.setImageResource(R.drawable.cross)
        }
    }

    fun applyCustomizations(settings: Settings, sideMargin: Int) {
        searchAdapter.notifyDataSetChanged()
        if (!searchAdapter.isSearch)
            unsearch()
        layout.spanCount = settings["dock:columns", 5]
        searchAdapter.showLabels = settings["drawer:labels", true]
        with(recyclerView) {
            adapter = searchAdapter
            val p = sideMargin / 2
            setPadding(p, p.coerceAtLeast(Utils.getStatusBarHeight(context)), p, p)
        }
        val fgColor = ColorThemer.drawerForeground(context)
        val hintColor = ColorThemer.drawerHint(context)
        val separatorColor = hintColor and 0xffffff or 0x44000000
        val dp = context.resources.displayMetrics.density
        val h = (sideMargin - (8 * dp).toInt()).coerceAtLeast(0)
        with(entry) {
            val v = (12 * dp).toInt()
            setPadding(h, v, h / 2, v)
            setTextColor(fgColor)
            setHintTextColor(hintColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                textCursorDrawable?.setTint(fgColor)
            highlightColor = fgColor and 0xffffff or 0x33000000
        }
        bottomBar.setPadding(0, 0, h, Utils.getNavigationBarHeight(context)
            .coerceAtLeast(sideMargin))
        extraButton.imageTintList = ColorStateList.valueOf(fgColor)
        separator.background = FillDrawable(separatorColor)
        icSearch.setTint(hintColor)
    }
}