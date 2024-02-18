package one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity

class IconPackViewHolder(context: Context, val type: Int) : RecyclerView.ViewHolder(LinearLayout(context)) {

    val icon = ImageView(context)
    val text = TextView(context)

    init {
        val dp = context.resources.displayMetrics.density
        with(itemView as LinearLayout) {
            val h = (20 * dp).toInt()
            val v = (8 * dp).toInt()
            setPadding(h, v, h, v)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val s = (32 * dp).toInt()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(icon, LayoutParams(s, s))
            addView(text, MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (12 * dp).toInt()
            })
        }
        with(text) {
            textSize = 18f
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(resources.getColor(R.color.color_text))
        }
    }

    fun bind(iconPack: IconPackPickerActivity.IconPack) {
        text.text = iconPack.label
        icon.setImageDrawable(iconPack.icon)
    }
}