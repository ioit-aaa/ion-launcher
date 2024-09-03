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

class HiddenAppsAdapter(
    private val showLabels: Boolean,
    private val activity: Activity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var items = emptyList<LauncherItem>()

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

    override fun getItemCount() = items.size + 1

    private fun getItem(i: Int) = items[i - 1]

    override fun getItemViewType(i: Int) = when {
        i == 0 -> 2
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val dp = parent.context.resources.displayMetrics.density
        if (type == 2) {
            return TitleViewHolder(TextView(parent.context).apply {
                textSize = 52f
                gravity = Gravity.CENTER
                setText(R.string.hidden_apps)
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
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
            icon.setPadding(0, (12 * dp).toInt(), 0, (10 * dp).toInt())
            addView(icon, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, iconSize + (22 * dp).toInt()))
            val p = (12 * dp).toInt()
            if (showLabels) addView(label.apply {
                gravity = Gravity.CENTER_HORIZONTAL
                textSize = 12f
                includeFontPadding = false
                setPadding(p, 0, p, 0)
            })
            setPadding(0, 0, 0, p)
        }
        return ViewHolder(view, icon, label).apply {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                item.open(it)
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
                LongPressMenu.onDragEnded()
                true
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == 0) return
        holder as ViewHolder
        val context = holder.itemView.context
        val item = getItem(i)
        holder.label.text = LabelLoader.loadLabel(context, item)
        holder.icon.setImageDrawable(IconLoader.loadIcon(context, item))
    }

    fun update(items: List<LauncherItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}