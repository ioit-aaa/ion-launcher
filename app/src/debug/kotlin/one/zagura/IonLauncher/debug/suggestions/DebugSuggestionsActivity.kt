package one.zagura.IonLauncher.debug.suggestions

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class DebugSuggestionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val dp = resources.displayMetrics.density

        val recycler = RecyclerView(this).apply {
            val p = (8 * dp).toInt()
            setPadding(p, p + Utils.getStatusBarHeight(context), p, p + Utils.getNavigationBarHeight(context))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        setContentView(recycler)

        Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)

        recycler.adapter = SuggestionsAdapter().apply {
            update(SuggestionsManager.getResource())
        }
    }
}