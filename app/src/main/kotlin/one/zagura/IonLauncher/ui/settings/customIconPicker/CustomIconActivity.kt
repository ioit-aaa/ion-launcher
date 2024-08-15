package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.EditedItems
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class CustomIconActivity : Activity() {

    companion object {
        @SuppressLint("InlinedApi")
        fun start(context: Context, item: LauncherItem) {
            context.startActivity(Intent(context, CustomIconActivity::class.java)
                .putExtra("item", item.toString()))
        }
    }

    lateinit var item: LauncherItem

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        item = LauncherItem.decode(this, intent.getStringExtra("item")
            ?: return finish())
            ?: return finish()

        val dp = resources.displayMetrics.density

        val recycler = RecyclerView(this).apply {
            val p = (16 * dp).toInt()
            setPadding(0, 0, 0, p + Utils.getNavigationBarHeight(context))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        setContentView(recycler)

        Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)

        val iconPacks = IconPackInfo.getAvailableIconPacks(packageManager)
        recycler.adapter = IconPackSourcesAdapter(item, iconPacks) {
            if (it == null) {
                EditedItems.resetIcon(this, item)
                finish()
            }
            else CustomIconPickerActivity.start(this, 0, it)
        }
    }

    @SuppressLint("InlinedApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != 0 || resultCode != RESULT_OK || data == null)
            return
        EditedItems.setIconPackIcon(this, item,
            data.getStringExtra(Intent.EXTRA_PACKAGE_NAME) ?: return,
            data.getStringExtra("icon") ?: return)
        finish()
    }
}