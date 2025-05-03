package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.provider.items.AppCategorizer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.CategoryBoxView
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils
import kotlin.math.abs
import kotlin.math.min

class CategoryAdapter(
    val showDropTargets: () -> Unit,
    val onItemOpened: (LauncherItem) -> Unit,
    val activity: Activity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var showLabels = false
    var category = AppCategorizer.AppCategory.AllApps
        private set
    private var apps = emptyList<LauncherItem>()

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)

    class TitleViewHolder(
        text: TextView,
    ) : RecyclerView.ViewHolder(text)

    override fun getItemId(i: Int) =
        if (i == 0) Long.MAX_VALUE
        else getItem(i).hashCode().toLong()

    override fun getItemCount() = apps.size + 1

    private fun getItem(i: Int) = apps[i - 1]

    override fun getItemViewType(i: Int) = when {
        i == 0 -> 1
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val dp = parent.context.resources.displayMetrics.density
        val settings = parent.context.ionApplication.settings
        val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
        if (type == 1) {
            return TitleViewHolder(TextView(parent.context).apply {
                textSize = 42f
                gravity = Gravity.START or Gravity.BOTTOM
                setTextColor(ColorThemer.drawerForeground(context))
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    typeface = Typeface.create(null, 500, false)
                val columns = settings["dock:columns", 5]
                val l = ((parent.width - parent.paddingLeft - parent.paddingRight) / columns - iconSize) / 2
                setPadding(l, l, l, l)
                setOnClickListener { parent.performClick() }
            })
        }
        val icon = ImageView(parent.context)
        val label = TextView(parent.context)
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            icon.setPadding(0, (12 * dp).toInt(), 0, (10 * dp).toInt())
            addView(icon, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, iconSize + (22 * dp).toInt()))
            val p = (12 * dp).toInt()
            if (showLabels) addView(label.apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(ColorThemer.drawerForeground(context))
                gravity = Gravity.CENTER_HORIZONTAL
                textSize = 12f
                includeFontPadding = false
                setPadding(p / 4, 0, p / 4, 0)
            })
            setPadding(0, 0, 0, p)
        }
        return ViewHolder(view, icon, label).apply {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                item.open(it)
                onItemOpened(item)
            }
            itemView.setOnLongClickListener {
                val dp = it.resources.displayMetrics.density
                val item = getItem(bindingAdapterPosition)
                val (lx, ly) = IntArray(2).apply(icon::getLocationInWindow)
                val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
                LongPressMenu.popupIcon(it, item, lx + (it.width - iconSize) / 2, ly + icon.paddingTop, iconSize, LongPressMenu.Where.DRAWER)
                Utils.startDrag(it, item, it to bindingAdapterPosition)
                true
            }
            itemView.setOnDragListener { v, e ->
                if (e.action == DragEvent.ACTION_DRAG_EXITED) {
                    if (e.localState == v to bindingAdapterPosition) {
                        LongPressMenu.dismissCurrent()
                        showDropTargets()
                    }
                } else if (e.action == DragEvent.ACTION_DRAG_ENDED) {
                    if (e.localState == v to bindingAdapterPosition)
                        LongPressMenu.onDragEnded()
                }
                true
            }
        }
    }

    private var currentAnimationPopup: PopupWindow? = null
    private var sourceX = 0
    private var sourceY = 0
    fun animateExitTransition(recyclerView: RecyclerView) {
        if (sourceX == 0 && sourceY == 0)
            return
        recyclerView.pivotX = sourceX.toFloat()
        recyclerView.pivotY = sourceY.toFloat()
        recyclerView.animate()
            .scaleX(0.5f).scaleY(0.5f)
            .setInterpolator(AccelerateInterpolator())
            .duration = 150L
        sourceY = 0
        sourceX = 0
    }
    fun animateEnterTransition(view: CategoryBoxView, recyclerView: RecyclerView) {
        currentAnimationPopup?.dismiss()
        val settings = view.context.ionApplication.settings
        val dp = view.resources.displayMetrics.density

        val columns = settings["dock:columns", 5]
        val iconSize = (settings["dock:icon-size", 48] * dp).toInt()

        val recyclerInnerWidth = recyclerView.width - recyclerView.paddingLeft - recyclerView.paddingRight
        val destY = recyclerView.paddingTop + calcTopMinHeight(view.context)
            .coerceAtLeast((recyclerInnerWidth / columns - iconSize) +
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 42f, view.resources.displayMetrics).toInt()) +
                (12 * dp).toInt()

        val insideIcons = view.getInsideIcons()
        if (insideIcons.isEmpty())
            return

        val iconBounds = view.getIconBounds(1, 1)
        val loc = IntArray(2).apply(view::getLocationOnScreen)
        sourceX = iconBounds.left + loc[0]
        sourceY = iconBounds.top + loc[1]

        recyclerView.pivotX = sourceX.toFloat()
        recyclerView.pivotY = sourceY.toFloat()
        recyclerView.scaleX = 0.7f
        recyclerView.scaleY = 0.7f
        recyclerView.animate()
            .scaleX(1f).scaleY(1f)
            .setInterpolator { it * it }
            .duration = 170L

        val content = FrameLayout(view.context)
        val popup = PopupWindow(content, view.resources.displayMetrics.widthPixels, abs(sourceY - destY) + iconSize, false)
        popup.setOnDismissListener { currentAnimationPopup = null }

        for ((i, icon) in insideIcons.take(columns.coerceAtMost(4)).withIndex())
            content.addView(View(view.context).apply {
                translationX = sourceX.toFloat()
                translationY = (sourceY - destY).coerceAtLeast(0).toFloat()
                background = icon
                scaleX = .45f
                scaleY = .45f
                val x = i % CategoryBoxView.GRID_SIZE
                val y = i / CategoryBoxView.GRID_SIZE
                pivotX = x * iconBounds.right.toFloat()
                pivotY = y * iconBounds.bottom.toFloat()
                animate()
                    .translationX(recyclerView.paddingLeft + i * recyclerInnerWidth / columns.toFloat() + (recyclerInnerWidth / columns - iconSize) / 2)
                    .translationY((destY - sourceY).coerceAtLeast(0).toFloat())
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(0f)
                    .setInterpolator(DecelerateInterpolator())
                    .also { if (i == 0) it.withEndAction { popup.dismiss() } }
                    .duration = 200L
            }, iconBounds.right, iconBounds.bottom)
        currentAnimationPopup = popup
        popup.showAtLocation(view, Gravity.START or Gravity.TOP, 0, min(sourceY, destY))
        sourceX += iconSize / 2
        sourceY += iconSize / 2
    }

    private fun calcTopMinHeight(context: Context): Int {
        val settings = context.ionApplication.settings
        val columns = settings["dock:columns", 5]
        val dp = context.resources.displayMetrics.density
        val contentHeight = apps.size / columns * ((settings["dock:icon-size", 48] + 60) * dp)
        return ((Utils.getDisplayHeight(activity) - contentHeight.toInt()) / 2)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0) {
            with(holder.itemView as TextView) {
                text = CategoryBoxView.getNameForCategory(holder.itemView.context, category)
                minHeight = calcTopMinHeight(holder.itemView.context)
            }
            return
        }
        holder as ViewHolder
        holder.label.text = ""
        holder.icon.setImageDrawable(null)
        holder.itemView.alpha = 0f
        TaskRunner.submit {
            val context = holder.itemView.context
            val item = getItem(i)
            if (showLabels) {
                val label = LabelLoader.loadLabel(context, item)
                holder.itemView.post {
                    holder.label.text = label
                }
            }
            val icon = IconLoader.loadIcon(context, item)
            holder.itemView.post {
                holder.icon.setImageDrawable(icon)
                holder.itemView.animate().alpha(1f).duration = 100L
            }
        }
    }

    fun update(category: AppCategorizer.AppCategory, apps: List<App>) {
        this.apps = apps
        this.category = category
        notifyDataSetChanged()
    }
}