package one.zagura.IonLauncher.debug.suggestions

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
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader

class SuggestionsAdapter : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    var items = emptyList<LauncherItem>()

    class ViewHolder(
        view: View,
        val icon: ImageView,
        val label: TextView,
    ) : RecyclerView.ViewHolder(view)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val dp = parent.context.resources.displayMetrics.density
        val icon = ImageView(parent.context).apply {
            val s = (32 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        val label = TextView(parent.context).apply {
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(resources.getColor(R.color.color_text))
            layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (12 * dp).toInt()
            }
        }
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val v = (12 * dp).toInt()
            val h = (12 * dp).toInt()
            setPadding(h, v, h, v)
            addView(icon)
            addView(label)
        }
        return ViewHolder(view, icon, label)
    }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        val context = holder.itemView.context
        val item = items[i]
        holder.icon.setImageDrawable(IconLoader.loadIcon(context, item))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item is StaticShortcut) {
            holder.label.text = buildSpannedString {
                append(LabelLoader.loadLabel(context, item.packageName, item.userHandle) + ": ", ForegroundColorSpan(holder.itemView.resources.getColor(R.color.color_hint)), 0)
                append(LabelLoader.loadLabel(context, item))
            }
        } else {
            holder.label.text = LabelLoader.loadLabel(context, item)
        }
    }

    fun update(items: List<LauncherItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}