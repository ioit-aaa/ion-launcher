package one.zagura.IonLauncher.ui.view.settings

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.util.Utils

class TitleViewHolder(context: Context) : RecyclerView.ViewHolder(TextView(context)) {

    init {
        with(itemView as TextView) {
            textSize = 42f
            gravity = Gravity.START or Gravity.BOTTOM
            val dp = context.resources.displayMetrics.density
            val h = (20 * dp).toInt()
            setPadding(h, Utils.getStatusBarHeight(context).coerceAtLeast((64 * dp).toInt()), h, h)
            setTextColor(resources.getColor(R.color.color_hint))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                typeface = Typeface.create(null, 500, false)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (256 * dp).toInt() + Utils.getStatusBarHeight(context)
            )
        }
    }

    fun bind(string: String) {
        (itemView as TextView).text = string
    }
}