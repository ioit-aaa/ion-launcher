package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
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
import one.zagura.IonLauncher.util.Utils

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
        if (type == 1) {
            return TitleViewHolder(TextView(parent.context).apply {
                textSize = 42f
                gravity = Gravity.CENTER
                setTextColor(ColorThemer.drawerForeground(context))
                val h = (256 * dp).toInt().coerceAtMost(Utils.getDisplayHeight(activity) / 4)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, h)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    typeface = Typeface.create(null, 300, false)
            })
        }
        val settings = parent.context.ionApplication.settings
        val icon = ImageView(parent.context)
        val label = TextView(parent.context)
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
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
                val sh = it.resources.displayMetrics.heightPixels
                val (lx, ly) = IntArray(2).apply(icon::getLocationInWindow)
                val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
                val h = lx + (-2 * dp).toInt() + (it.width - iconSize) / 2
                val v = (8 * dp).toInt()
                if (ly > sh / 2) LongPressMenu.popup(
                    it, item,
                    Gravity.BOTTOM or Gravity.START,
                    h, Utils.getDisplayHeight(activity) - ly + v,
                    LongPressMenu.Where.DRAWER,
                ) else LongPressMenu.popup(
                    it, item,
                    Gravity.TOP or Gravity.START,
                    h, ly + icon.height + v,
                    LongPressMenu.Where.DRAWER,
                )
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0) {
            holder as TitleViewHolder
            (holder.itemView as TextView).text = CategoryBoxView.getNameForCategory(holder.itemView.context, category)
            return
        }
        holder as ViewHolder
        val context = holder.itemView.context
        val item = getItem(i)
        if (showLabels) {
            val label = LabelLoader.loadLabel(context, item)
            holder.label.text = label
        }
        holder.icon.setImageDrawable(IconLoader.loadIcon(context, item))
    }

    fun update(category: AppCategorizer.AppCategory, apps: List<App>) {
        this.apps = apps
        this.category = category
        notifyDataSetChanged()
    }
}