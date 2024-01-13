package one.zagura.IonLauncher.ui.settings.suggestions

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.Utils

class SuggestionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.setDecorFitsSystemWindows(false)
        else
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        window.setBackgroundDrawable(FillDrawable(ColorThemer.COLOR_CARD))
        val bc = ColorThemer.COLOR_CARD and 0xffffff or 0x55000000
        window.statusBarColor = bc
        window.navigationBarColor = bc

        val dp = resources.displayMetrics.density

        val recycler = RecyclerView(this).apply {
            val p = (8 * dp).toInt()
            setPadding(p, p + Utils.getStatusBarHeight(context), p, p + Utils.getNavigationBarHeight(context))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        setContentView(recycler)

        recycler.adapter = SuggestionsAdapter().apply {
            update(SuggestionsManager.getResource())
        }
    }
}