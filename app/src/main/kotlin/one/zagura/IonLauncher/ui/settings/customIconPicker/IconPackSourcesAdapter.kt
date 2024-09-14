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
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder

class IconPackSourcesAdapter(
    val item: LauncherItem,
    val iconPacks: List<ResolveInfo>,
    val onSelected: (String?) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)

    override fun getItemCount() = iconPacks.size + 2

    override fun getItemViewType(i: Int) = when (i) {
        0 -> 0
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        if (type == 0)
            return TitleViewHolder(parent.context).apply {
                bind(parent.context.getString(R.string.icon_packs))
            }
        val dp = parent.resources.displayMetrics.density
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
                onSelected(
                    if (bindingAdapterPosition == 1) null
                    else iconPacks[bindingAdapterPosition - 2].activityInfo.packageName)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0)
            return
        holder as ViewHolder
        if (i == 1) {
            holder.icon.setImageDrawable(null)
            holder.label.setText(R.string.reset)
            return
        }
        val iconPack = iconPacks[i - 2]
        holder.icon.setImageDrawable(iconPack.loadIcon(holder.icon.context.packageManager))
        holder.label.text = iconPack.loadLabel(holder.icon.context.packageManager)
    }
}