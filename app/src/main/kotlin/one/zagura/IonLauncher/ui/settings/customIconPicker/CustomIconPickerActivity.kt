package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class CustomIconPickerActivity : Activity() {

    companion object {
        @SuppressLint("InlinedApi")
        fun start(activity: Activity, requestCode: Int, packageName: String) {
            activity.startActivityForResult(Intent(activity, CustomIconPickerActivity::class.java)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName), requestCode)
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) ?: return finish()
        val res = packageManager.getResourcesForApplication(packageName)
        val items = IconPackInfo.getResourceNames(res, packageName)

        val dp = resources.displayMetrics.density
        val sideMargin = calculateSideMargin(this)

        val recycler = RecyclerView(this).apply {
            val p = (16 * dp).toInt()
            setPadding(sideMargin / 2, 0, sideMargin / 2, p + Utils.getNavigationBarHeight(context))
            val columns = ionApplication.settings["dock:columns", 5]
            layoutManager = GridLayoutManager(context, columns, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(i: Int) = if (i == 0 || items[i - 1] is IconPackInfo.IconPackResourceItem.Title) columns else 1
                }
            }
        }
        setContentView(recycler)

        Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)

        recycler.adapter = IconPackIconsAdapter(res, packageName, items, sideMargin) {
            setResult(RESULT_OK, Intent()
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                .putExtra("icon", it))
            finish()
        }
    }

    fun calculateSideMargin(context: Context): Int {
        val w = context.resources.displayMetrics.widthPixels
        val dp = context.resources.displayMetrics.density
        val settings = context.ionApplication.settings
        val iconSize = (settings["dock:icon-size", 48] * dp).toInt()
        val columns = settings["dock:columns", 5]
        return (w - iconSize * columns) / (columns + 1)
    }
}