package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.content.res.Resources
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.IconThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder

class IconPackIconsAdapter(
    val res: Resources,
    val packageName: String,
    val items: List<IconPackInfo.IconPackResourceItem>,
    private val sideMargin: Int,
    val onSelected: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(val icon: ImageView) : RecyclerView.ViewHolder(icon)
    class CategoryViewHolder(val text: TextView) : RecyclerView.ViewHolder(text)

    override fun getItemCount() = items.size + 1

    override fun getItemViewType(i: Int) = when (i) {
        0 -> 0
        else -> if (items[i - 1] is IconPackInfo.IconPackResourceItem.Title) 2 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        if (type == 0)
            return TitleViewHolder(parent.context).apply {
                val pm = parent.context.packageManager
                bind(pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString())
            }
        val dp = parent.resources.displayMetrics.density
        if (type == 2)
            return CategoryViewHolder(TextView(parent.context).apply {
                setPadding(sideMargin / 2 + (4 * dp).toInt(), sideMargin, sideMargin / 2, sideMargin / 4)
                textSize = 20f
                ellipsize = android.text.TextUtils.TruncateAt.END
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.02f
                setTextColor(resources.getColor(R.color.color_heading))
            })
        val settings = parent.context.ionApplication.settings
        val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
        return ViewHolder(ImageView(parent.context).apply {
            setPadding(0, sideMargin / 2, 0, sideMargin / 2)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, iconSize + sideMargin)
        }).apply {
            itemView.setOnClickListener {
                val icon = items[bindingAdapterPosition - 1]
                if (icon is IconPackInfo.IconPackResourceItem.Icon)
                    onSelected(icon.string)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0)
            return
        val item = items[i - 1]
        when (holder) {
            is CategoryViewHolder -> holder.text.text = item.string
            is ViewHolder -> {
                val density = holder.icon.context.resources.displayMetrics.densityDpi
                holder.icon.setImageDrawable(IconPackInfo.fromResourceName(res, packageName, item.string, density)
                    ?.let(IconThemer::transformIconFromIconPack))
            }
        }
    }
}