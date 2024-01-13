package one.zagura.IonLauncher.ui

import android.os.Build
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
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
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.items.IconLoader
import one.zagura.IonLauncher.util.Utils

class SearchAdapter(
    val showDropTargets: () -> Unit,
    val onItemOpened: (LauncherItem) -> Unit,
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var items = emptyList<LauncherItem>()

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
            val iconSize = (HomeScreen.SEARCH_ICON_SIZE * dp).toInt()
            setPadding((12 * dp).toInt())
            addView(icon, LinearLayout.LayoutParams(iconSize, iconSize))
            addView(label)
        }
        return ViewHolder(view, icon, label).apply {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                item.open(it)
                onItemOpened(item)
            }
            itemView.setOnLongClickListener {
                val item = getItem(bindingAdapterPosition)
                Utils.startDrag(it, item, null)
                showDropTargets()
                true
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        val item = getItem(i)
        with(holder.label) {
            text = item.label
            setTextColor(ColorThemer.foreground(holder.itemView.context))
        }
        holder.icon.setImageDrawable(IconLoader.loadIcon(holder.itemView.context, item))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item is StaticShortcut) {
            holder.label.text = buildSpannedString {
                append(item.appLabel + ": ", ForegroundColorSpan(ColorThemer.hint(holder.itemView.context)), 0)
                append(item.label)
            }
        }
    }

    fun update(items: List<LauncherItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}