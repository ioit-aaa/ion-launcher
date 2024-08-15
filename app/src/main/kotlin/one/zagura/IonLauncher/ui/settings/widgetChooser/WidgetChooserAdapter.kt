package one.zagura.IonLauncher.ui.settings.widgetChooser

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.view.settings.TitleViewHolder

class WidgetChooserAdapter(
    private val providers: List<AppWidgetProviderInfo>,
    private val activity: WidgetChooserActivity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class VH(
        view: View,
        val label: TextView,
        val preview: ImageView,
        val icon: ImageView
    ) : RecyclerView.ViewHolder(view)

    override fun getItemCount() = providers.size + 2

    override fun getItemViewType(position: Int) = when (position) {
        0 -> 1
        1 -> 2
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        if (type == 1)
            return TitleViewHolder(parent.context)
        val dp = parent.resources.displayMetrics.density
        val label = TextView(parent.context).apply {
            setTextColor(resources.getColor(R.color.color_text))
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
        }
        val context = parent.context
        val preview = ImageView(context).apply {
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
        }
        val icon = ImageView(context)
        val v = LinearLayout(context).apply {
            val p = (16 * dp).toInt()
            setPadding(0, p, 0, p)
            layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            val h = (48 * dp).toInt()
            addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, parent.resources.displayMetrics.widthPixels / 2 - h))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val p = (8 * dp).toInt()
                setPadding(p, p, p, p)
                val hh = h - p * 2
                addView(icon, LinearLayout.LayoutParams(hh, hh).apply {
                    marginEnd = p
                })
                addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h))
        }
        return VH(v, label, preview, icon).apply {
            if (type == 0) itemView.setOnClickListener {
                val p = providers[bindingAdapterPosition - 2]
                activity.requestAddWidget(p)
            }
            else if (type == 2) itemView.setOnClickListener {
                activity.requestRemoveWidget()
            }
        }
    }

    private val iconCache = arrayOfNulls<Drawable>(providers.size)
    private val previewCache = arrayOfNulls<Drawable>(providers.size)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (holder is TitleViewHolder)
            return holder.bind(holder.itemView.context.getString(R.string.choose_widget))
        val context = holder.itemView.context
        holder as VH

        val dp = context.resources.displayMetrics.density
        val a = (8 * dp).toInt()
        holder.itemView.updateLayoutParams<MarginLayoutParams> {
            if (i % 2 == 0)
                setMargins(a, a * 2, a * 2, 0)
            else
                setMargins(a * 2, a * 2, a, 0)
        }

        if (i == 1) {
            holder.icon.setImageDrawable(null)
            holder.icon.isVisible = false
            holder.preview.setImageDrawable(null)
            holder.label.text = context.getString(R.string.nothing)
            return
        }
        holder.icon.isVisible = true
        val p = providers[i - 2]
        val pm = context.packageManager
        holder.label.text = p.loadLabel(pm)
        holder.icon.setImageDrawable(iconCache[i - 2]
            ?: p.loadIcon(context, context.resources.displayMetrics.densityDpi).also { iconCache[i - 2] = it })
        holder.preview.setImageDrawable(previewCache[i - 2]
            ?: (p.loadPreviewImage(context, context.resources.displayMetrics.densityDpi) ?: iconCache[i - 2]).also { previewCache[i - 2] = it })
    }
}