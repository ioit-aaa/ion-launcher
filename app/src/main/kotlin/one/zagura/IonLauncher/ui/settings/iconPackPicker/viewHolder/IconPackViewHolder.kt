package one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import one.zagura.IonLauncher.ui.settings.iconPackPicker.IconPackPickerAdapter

class IconPackViewHolder(context: Context, val type: Int) : RecyclerView.ViewHolder(LinearLayout(context)) {

    val icon = ImageView(context)
    val text = TextView(context)
    val dragIndicator = ImageView(context)

    init {
        val dp = context.resources.displayMetrics.density
        with(itemView as LinearLayout) {
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
            if (type != IconPackPickerAdapter.SYSTEM_ICON_PACK) {
                addView(dragIndicator, LayoutParams(s, LayoutParams.MATCH_PARENT))
                with(dragIndicator) {
                    setImageResource(R.drawable.hint_drag)
                    imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_hint))
                }
            }
        }
        with(text) {
            val v = (12 * dp).toInt()
            setPadding(0, v, 0, v)
            textSize = 20f
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(resources.getColor(R.color.color_text))
        }
    }

    fun bind(iconPack: IconPackPickerActivity.IconPack) {
        text.text = iconPack.label
        icon.setImageDrawable(iconPack.icon)
    }
}