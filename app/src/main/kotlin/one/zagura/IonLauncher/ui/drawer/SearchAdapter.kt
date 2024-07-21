package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.provider.items.LabelLoader
import one.zagura.IonLauncher.ui.HomeScreen
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.util.Utils

class SearchAdapter(
    val showDropTargets: () -> Unit,
    val onItemOpened: (LauncherItem) -> Unit,
    val activity: Activity,
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var items = emptyList<LauncherItem>()
    private val transparent = ColorStateList.valueOf(0)
    private var isSearch = false

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)

    override fun getItemId(i: Int) = getItem(i).hashCode().toLong() // sus

    override fun getItemCount() = items.size

    private fun getItem(i: Int) = items[i]

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val dp = parent.context.resources.displayMetrics.density
        val icon = ImageView(parent.context)
        val label = TextView(parent.context).apply {
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (12 * dp).toInt()
            }
        }
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val iconSize = (40 * dp).toInt()
            setPadding((12 * dp).toInt())
            addView(icon, LinearLayout.LayoutParams(iconSize, iconSize))
            addView(label)
            val iconRadius = iconSize * parent.context.ionApplication.settings["icon:radius-ratio", 50] / 100f
            val r = if (iconRadius == 0f) 0f else iconRadius + 12f * dp
            background = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
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
                val h = (-2 * dp).toInt()
                val v = (8 * dp).toInt()
                if (ly > sh / 2) {
                    LongPressMenu.popup(
                        it, item,
                        Gravity.BOTTOM or Gravity.START,
                        lx + h, Utils.getDisplayHeight(activity) - ly + v
                    )
                } else {
                    LongPressMenu.popup(
                        it, item,
                        Gravity.TOP or Gravity.START,
                        lx + h, ly + icon.height + v
                    )
                }
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

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        val context = holder.itemView.context
        val item = getItem(i)
        holder.itemView.backgroundTintList = if (isSearch && i == 0)
            ColorStateList.valueOf(ColorThemer.highlight(context))
        else transparent
        with(holder.label) {
            setTextColor(ColorThemer.foreground(context))
        }
        holder.icon.setImageDrawable(IconLoader.loadIcon(context, item))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item is StaticShortcut) {
            holder.label.text = buildSpannedString {
                append(LabelLoader.loadLabel(context, item.packageName, item.userHandle) + ": ", ForegroundColorSpan(ColorThemer.hint(context)), 0)
                append(LabelLoader.loadLabel(context, item))
            }
        } else {
            holder.label.text = LabelLoader.loadLabel(context, item)
        }
    }

    fun update(items: List<LauncherItem>, isSearch: Boolean) {
        this.items = items
        this.isSearch = isSearch
        notifyDataSetChanged()
    }
}