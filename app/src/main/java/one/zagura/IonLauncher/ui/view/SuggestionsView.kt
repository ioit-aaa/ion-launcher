package one.zagura.IonLauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.TextUtils
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.Dock
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import java.util.ArrayList

@SuppressLint("ViewConstructor")
class SuggestionsView(
    context: Context,
    val showDropTargets: () -> Unit,
) : LinearLayout(context) {

    private var sideButton: View? = null
    private var lastSuggestions = emptyList<LauncherItem>()

    fun update(allSuggestions: List<LauncherItem>) {
        context.ionApplication.task {
            val suggestions = loadSuggestions(allSuggestions)
            if (suggestions.isEmpty()) post {
                removeAllViews()
                isVisible = false
            }
            else if (lastSuggestions == suggestions) post {
                isVisible = true
                return@post
            }
            else post {
                val dp = resources.displayMetrics.density
                val height = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
                val l = LayoutParams(0, height, 1f).apply {
                    marginStart = (12 * dp).toInt()
                }
                removeAllViews()
                for ((i, s) in suggestions.withIndex()) {
                    addView(createItemView(s, i, suggestions.size), if (i == 0) LayoutParams(0, height, 1f) else l)
                }
                if (sideButton != null)
                    addView(sideButton)
                isVisible = true
            }
        }
    }

    fun applyCustomizations(settings: Settings, onSearch: (View) -> Unit) {
        if (!settings["layout:search-in-suggestions", false])
            sideButton = null
        else {
            val dp = resources.displayMetrics.density
            sideButton = ImageView(context).apply {
                setImageResource(R.drawable.ic_search)
                val p = (8 * dp).toInt()
                setPadding(p, p, p, p)
                val r = 99 * dp
                background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
                backgroundTintList = ColorStateList.valueOf(ColorThemer.pillBackground(context))
                imageTintList = ColorStateList.valueOf(ColorThemer.pillForeground(context))
                val height = (settings["dock:icon-size", 48] * dp).toInt()
                layoutParams = LayoutParams(height, height).apply {
                    marginStart = (12 * dp).toInt()
                }
                setOnClickListener(onSearch)
            }
        }
    }

    private fun createItemView(s: LauncherItem, i: Int, columns: Int): View = LinearLayout(context).apply {
        val dp = resources.displayMetrics.density
        orientation = HORIZONTAL
        val r = 99 * dp
        background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        backgroundTintList = ColorStateList.valueOf(ColorThemer.pillBackground(context))
        val height = (context.ionApplication.settings["dock:icon-size", 48] * dp).toInt()
        val p = (8 * dp).toInt()
        addView(ImageView(context).apply {
            setImageDrawable(IconLoader.loadIcon(context, s))
            setPadding(p, p, 0, p)
        }, LayoutParams(height - p, LayoutParams.MATCH_PARENT))
        addView(TextView(context).apply {
            text = s.label
            gravity = Gravity.CENTER_VERTICAL
            ellipsize = TextUtils.TruncateAt.END
            setSingleLine()
            setTextColor(ColorThemer.pillForeground(context))
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            val m = (4 * dp).toInt()
            leftMargin = m
            rightMargin = m
        })
        setOnClickListener(s::open)
        setOnLongClickListener {
            LongPressMenu.popup(
                it, s,
                Gravity.BOTTOM or Gravity.START,
                this@SuggestionsView.paddingLeft + (this@SuggestionsView.width - this@SuggestionsView.paddingLeft - this@SuggestionsView.paddingRight) / columns * i,
                this@SuggestionsView.height + Utils.getNavigationBarHeight(it.context) + (4 * dp).toInt()
            )
            Utils.startDrag(it, s, it)
            showDropTargets()
            true
        }
        setOnDragListener { v, e ->
            if (e.action == DragEvent.ACTION_DRAG_EXITED) {
                if (e.localState == v)
                    LongPressMenu.dismissCurrent()
            }
            true
        }
    }

    private fun loadSuggestions(allSuggestions: List<LauncherItem>): List<LauncherItem> {
        val suggestionCount = context.ionApplication.settings["suggestion:count", 3]
        if (suggestionCount == 0)
            return emptyList()
        val dockItems = Dock.getItems(context)
        val suggestions = ArrayList<LauncherItem>(suggestionCount)
        for (s in allSuggestions) {
            if (dockItems.contains(s))
                continue
            suggestions.add(s)
            if (suggestions.size == suggestionCount)
                break
        }
        return suggestions
    }
}