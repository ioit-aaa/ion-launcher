package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.util.Utils

class SearchAdapter(
    val showDropTargets: () -> Unit,
    val onItemOpened: (LauncherItem) -> Unit,
    val activity: Activity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var showLabels = false
    private var items = emptyList<LauncherItem>()
    private val transparent = ColorStateList.valueOf(0)
    var isSearch = false
        private set

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)

    class TitleViewHolder(
        text: TextView,
    ) : RecyclerView.ViewHolder(text)

    override fun getItemId(i: Int) =
        if (!isSearch && i == 0) 0
        else getItem(i).hashCode().toLong() // sus

    override fun getItemCount() = if (isSearch) items.size else items.size + 1

    private fun getItem(i: Int) = items[if (isSearch) i else i - 1]

    override fun getItemViewType(i: Int) = when {
        isSearch -> 0
        i == 0 -> 2
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val dp = parent.context.resources.displayMetrics.density
        if (type == 2) {
            return TitleViewHolder(TextView(parent.context).apply {
                textSize = 52f
                gravity = Gravity.CENTER
                setText(R.string.all_apps)
                setTextColor(ColorThemer.drawerForeground(context))
                val h = (256 * dp).toInt().coerceAtMost(Utils.getDisplayHeight(activity) / 3)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, h)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    typeface = Typeface.create(null, 200, false)
            })
        }
        val settings = parent.context.ionApplication.settings
        val icon = ImageView(parent.context)
        val label = TextView(parent.context).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ColorThemer.drawerForeground(context))
        }
        val view = if (type == 0) LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val iconSize = (40 * dp).toInt()
            val p = (12 * dp).toInt()
            icon.setPadding(p)
            addView(icon, LinearLayout.LayoutParams(iconSize + p * 2, iconSize + p * 2))
            addView(label.apply {
                textSize = 16f
                layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (12 * dp).toInt()
                }
            })
            val iconRadius = iconSize * settings["icon:radius-ratio", 50] / 100f
            val r = if (iconRadius == 0f) 0f else iconRadius + 12f * dp
            background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        } else LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
            val p = (12 * dp).toInt()
            icon.setPadding(0, (12 * dp).toInt(), 0, (10 * dp).toInt())
            addView(icon, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, iconSize + (22 * dp).toInt()))
            if (showLabels) addView(label.apply {
                gravity = Gravity.CENTER_HORIZONTAL
                textSize = 12f
                includeFontPadding = false
                setPadding(p, 0, p, p)
            })
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
                    true,
                ) else LongPressMenu.popup(
                    it, item,
                    Gravity.TOP or Gravity.START,
                    h, ly + icon.height + v,
                    true,
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
        if (!isSearch && i == 0) return
        holder as ViewHolder
        val context = holder.itemView.context
        val item = getItem(i)
        val label = LabelLoader.loadLabel(context, item)
        if (isSearch) {
            holder.itemView.backgroundTintList = if (i == 0)
                ColorStateList.valueOf(ColorThemer.drawerHighlight(context))
            else transparent
            holder.label.text = if (isSearch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item is StaticShortcut)
                buildSpannedString {
                    append(LabelLoader.loadLabel(context, item.packageName, item.userHandle) + ": ", ForegroundColorSpan(ColorThemer.drawerHint(context)), 0)
                    append(label)
                }
            else label
        } else
            holder.label.text = label
        holder.icon.setImageDrawable(IconLoader.loadIcon(context, item))
    }

    fun update(items: List<LauncherItem>, isSearch: Boolean) {
        this.items = items
        this.isSearch = isSearch
        notifyDataSetChanged()
    }
}