package one.zagura.IonLauncher.ui.drawer

import android.app.Activity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.PinnedGridView
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class HiddenAppsActivity : Activity() {

    private lateinit var appsAdapter: HiddenAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val settings = ionApplication.settings

        appsAdapter = HiddenAppsAdapter(settings["drawer:labels", true], this)

        val recycler = RecyclerView(this).apply {
            val p = PinnedGridView.calculateSideMargin(context) / 2
            setPadding(p, p.coerceAtLeast(Utils.getStatusBarHeight(context)), p, p)
            layoutManager = GridLayoutManager(context, settings["dock:columns", 5], RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(i: Int) =
                        if (i == 0) spanCount else 1
                }
            }
            adapter = appsAdapter
        }
        setContentView(recycler)
    }

    override fun onStart() {
        super.onStart()
        AppLoader.track {
            appsAdapter.update(HiddenApps.getItems(this))
        }
    }

    override fun onStop() {
        super.onStop()
        AppLoader.release()
    }
}