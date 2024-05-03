package one.zagura.IonLauncher.ui.view.settings

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.Utils

class TitleViewHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {

    private val text = TextView(context).apply {
        val dp = context.resources.displayMetrics.density
        gravity = Gravity.CENTER_VERTICAL
        val h = (20 * dp).toInt()
        setPadding(h, Utils.getStatusBarHeight(context), h, 0)
        textSize = 22f
        setTextColor(resources.getColor(R.color.color_hint))
        background = FillDrawable(resources.getColor(R.color.color_bg))
    }

    private val separator = View(context).apply {
        background = FillDrawable(resources.getColor(R.color.color_separator))
    }

    init {
        with(itemView as LinearLayout) {
            val dp = context.resources.displayMetrics.density
            orientation = LinearLayout.VERTICAL
            addView(text,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (64 * dp).toInt() + Utils.getStatusBarHeight(context)
                )
            )
            addView(separator,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp.toInt())
            )
        }
    }

    fun bind(string: String) {
        text.text = string
    }
}