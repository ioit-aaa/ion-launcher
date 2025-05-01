package one.zagura.IonLauncher.ui.settings.customIconPicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.EditedItems
import one.zagura.IonLauncher.provider.icons.IconLoader
import one.zagura.IonLauncher.provider.icons.IconPackInfo
import one.zagura.IonLauncher.provider.icons.IconThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.view.PinnedGridView
import one.zagura.IonLauncher.ui.view.settings.setupWindow
import one.zagura.IonLauncher.util.Utils

class CustomIconActivity : Activity() {

    companion object {
        @SuppressLint("InlinedApi")
        fun start(context: Context, item: LauncherItem) {
            context.startActivity(Intent(context, CustomIconActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("item", item.toString()))
        }
    }

    lateinit var item: LauncherItem

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val item = LauncherItem.decode(this, intent.getStringExtra("item")
            ?: return finish())
            ?: return finish()
        this.item = item

        val iconPacks = IconPackInfo.getAvailableIconPacks(packageManager)
        val defIcons = if (item is App) iconPacks.mapNotNull {
            val packageName = it.activityInfo.packageName
            val name = IconPackInfo.get(packageManager, packageName)
                .getDrawableName(item.packageName, item.name)
                ?: return@mapNotNull null
            packageName to name
        } else emptyList()

        val dp = resources.displayMetrics.density
        val sideMargin = PinnedGridView.calculateSideMargin(this)

        val recycler = RecyclerView(this).apply {
            val p = (16 * dp).toInt()
            setPadding(sideMargin / 2, 0, sideMargin / 2, p + Utils.getNavigationBarHeight(context))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val columns = ionApplication.settings["dock:columns", 5]
            layoutManager = GridLayoutManager(context, columns, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(i: Int) = if (i >= 1 && i < defIcons.size + 1) 1 else columns
                }
            }
        }
        setContentView(recycler)

        Utils.setDarkStatusFG(window, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)

        recycler.adapter = IconPackSourcesAdapter(
            defIcons,
            iconPacks,
            sideMargin,
            onSelected = {
                if (it == null) {
                    EditedItems.resetIcon(this, item)
                    finish()
                }
                else CustomIconPickerActivity.start(this, 0, it)
            },
            onIconSelected = { packageName, icon ->
                EditedItems.setIconPackIcon(this, item, packageName, icon)
                finish()
            },
        )
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