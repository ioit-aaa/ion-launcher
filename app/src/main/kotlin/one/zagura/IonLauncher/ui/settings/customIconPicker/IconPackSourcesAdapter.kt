package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.IconThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder

class IconPackSourcesAdapter(
    private val defIcons: List<Pair<String, String>>,
    private val iconPacks: List<ResolveInfo>,
    private val sideMargin: Int,
    val onSelected: (String?) -> Unit,
    val onIconSelected: (packageName: String, icon: String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)
    class IconViewHolder(val icon: ImageView) : RecyclerView.ViewHolder(icon)

    override fun getItemCount() = iconPacks.size + 2 + defIcons.size

    override fun getItemViewType(i: Int) = when (i) {
        0 -> 0
        else -> if (i < defIcons.size + 1) 2 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        if (type == 0)
            return TitleViewHolder(parent.context).apply {
                bind(parent.context.getString(R.string.icon_packs))
            }
        val dp = parent.resources.displayMetrics.density
        if (type == 2) {
            val settings = parent.context.ionApplication.settings
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
            return IconViewHolder(ImageView(parent.context).apply {
                setPadding(0, sideMargin / 2, 0, sideMargin / 2)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, iconSize + sideMargin)
            }).apply {
                itemView.setOnClickListener {
                    val (packageName, icon) = defIcons[bindingAdapterPosition - 1]
                    onIconSelected(packageName, icon)
                }
            }
        }
        val icon = ImageView(parent.context)
        val text = TextView(parent.context).apply {
            val v = (12 * dp).toInt()
            setPadding(0, v, 0, v)
            textSize = 20f
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(resources.getColor(R.color.color_text))
        }

        return ViewHolder(LinearLayout(parent.context).apply {
            val h = (20 * dp).toInt()
            setPadding(h, 0, h, 0)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val s = (32 * dp).toInt()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(icon, LayoutParams(s, s))
            addView(text, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = (12 * dp).toInt()
            })
            addView(ImageView(context).apply {
                setImageResource(R.drawable.arrow_right)
                imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_hint))
            }, LayoutParams(s, LayoutParams.MATCH_PARENT))
        }, icon, text).apply {
            itemView.setOnClickListener {
                val i = bindingAdapterPosition - 1 - defIcons.size
                onSelected(
                    if (i == 0) null
                    else iconPacks[i - 1].activityInfo.packageName)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0)
            return
        if (i < defIcons.size + 1) {
            holder as IconViewHolder
            val (packageName, icon) = defIcons[i - 1]
            val density = holder.icon.context.resources.displayMetrics.densityDpi
            val res = holder.icon.context.packageManager.getResourcesForApplication(packageName)
            holder.icon.setImageDrawable(IconPackInfo.fromResourceName(res, packageName, icon, density)
                ?.let(IconThemer::transformIconFromIconPack))
            return
        }
        holder as ViewHolder
        if (i == defIcons.size + 1) {
            holder.icon.setImageDrawable(null)
            holder.label.setText(R.string.reset)
            return
        }
        val iconPack = iconPacks[i - 2 - defIcons.size]
        holder.icon.setImageDrawable(iconPack.loadIcon(holder.icon.context.packageManager))
        holder.label.text = iconPack.loadLabel(holder.icon.context.packageManager)
    }
}