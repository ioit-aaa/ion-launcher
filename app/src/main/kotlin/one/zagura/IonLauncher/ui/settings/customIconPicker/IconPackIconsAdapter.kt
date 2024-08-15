package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.IconThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder

class IconPackIconsAdapter(
    val res: Resources,
    val packageName: String,
    val icons: List<String>,
    val sideMargin: Int,
    val onSelected: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(val icon: ImageView) : RecyclerView.ViewHolder(icon)

    override fun getItemCount() = icons.size + 1

    override fun getItemViewType(i: Int) = when (i) {
        0 -> 0
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        if (type == 0)
            return TitleViewHolder(parent.context).apply {
                val pm = parent.context.packageManager
                bind(pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString())
            }
        val dp = parent.resources.displayMetrics.density
        val settings = parent.context.ionApplication.settings
        val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
        return ViewHolder(ImageView(parent.context).apply {
            setPadding(0, sideMargin / 2, 0, sideMargin / 2)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, iconSize + sideMargin)
        }).apply {
            itemView.setOnClickListener {
                val icon = icons[bindingAdapterPosition - 1]
                onSelected(icon)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0)
            return
        holder as ViewHolder
        val icon = icons[i - 1]
        val density = holder.icon.context.resources.displayMetrics.densityDpi
        holder.icon.setImageDrawable(IconPackInfo.fromResourceName(res, packageName, icon, density)
            ?.let(IconThemer::transformIconFromIconPack))
    }
}