package one.zagura.IonLauncher.ui.settings.widgetChooser

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Process
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.Widget
import one.zagura.IonLauncher.provider.Widgets
import one.zagura.IonLauncher.ui.HomeScreen
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class WidgetChooserActivity : Activity() {

    companion object {
        const val REQUEST_CONFIGURE_WIDGET = 1
        const val REQUEST_BIND_WIDGET = 2
    }

    private lateinit var host: AppWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        val widgetManager = AppWidgetManager.getInstance(applicationContext)

        val dp = resources.displayMetrics.density

        val recycler = RecyclerView(this).apply {
            val p = (16 * dp).toInt()
            setPadding(0, 0, 0, p + Utils.getNavigationBarHeight(context))
            layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(i: Int) = if (i == 0) 2 else 1
                }
            }
        }
        setContentView(recycler)

        Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)

        host = AppWidgetHost(this, Widget.HOST_ID)

        val providers = widgetManager.installedProviders
        recycler.adapter = WidgetChooserAdapter(providers, this)
    }


    private var tmpProvider: AppWidgetProviderInfo? = null

    fun requestRemoveWidget() {
        Widgets.setWidget(this, null)
        startActivity(Intent(this, HomeScreen::class.java))
    }

    fun requestAddWidget(
        p: AppWidgetProviderInfo,
        id: Int = host.allocateAppWidgetId(),
        configured: Boolean = false,
    ) {
        val widgetManager = AppWidgetManager.getInstance(this)
        if (!configured && !widgetManager.bindAppWidgetIdIfAllowed(id, p.provider)) {
            tmpProvider = p
            startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, Process.myUserHandle())
            }, REQUEST_BIND_WIDGET)
            return
        }

        if (!configured && p.configure != null) {
            tmpProvider = p
            startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = p.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }, REQUEST_CONFIGURE_WIDGET)
            return
        }

        Widgets.setWidget(this, Widget(p.provider.packageName, p.provider.className, id))
        startActivity(Intent(this, HomeScreen::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val provider = tmpProvider
        tmpProvider = null
        if (resultCode == RESULT_CANCELED) {
            if (data != null) {
                val id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (id != -1)
                    host.deleteAppWidgetId(id)
            }
        }
        if (resultCode == RESULT_OK) when (requestCode) {
            REQUEST_BIND_WIDGET -> {
                val extras = data!!.extras!!
                val id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (id == -1 || provider == null)
                    return
                requestAddWidget(provider, id)
            }
            REQUEST_CONFIGURE_WIDGET -> {
                val extras = data!!.extras!!
                val id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (id == -1 || provider == null)
                    return
                requestAddWidget(provider, id, configured = true)
            }
        }
    }
}