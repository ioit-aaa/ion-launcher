package one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer

class SectionViewHolder(context: Context) : RecyclerView.ViewHolder(TextView(context)) {

    init {
        with(itemView as TextView) {
            val dp = context.resources.displayMetrics.density
            val h = (20 * dp).toInt()
            setPadding(h, (24 * dp).toInt(), h, (6 * dp).toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resources.getColor(R.color.color_hint))
        }
    }

    fun bind(string: String) {
        (itemView as TextView).text = string
    }
}