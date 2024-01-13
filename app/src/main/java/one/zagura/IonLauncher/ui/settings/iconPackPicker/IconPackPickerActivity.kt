package one.zagura.IonLauncher.ui.settings.iconPackPicker

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.ui.settings.iconPackPicker.viewHolder.IconPackViewHolder
import one.zagura.IonLauncher.util.FillDrawable
import one.zagura.IonLauncher.util.IconTheming
import one.zagura.IonLauncher.util.Utils
import java.util.*

class IconPackPickerActivity : Activity() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.setDecorFitsSystemWindows(false)
        else
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val settings = ionApplication.settings

        window.setBackgroundDrawable(FillDrawable(ColorThemer.COLOR_CARD))
        val bc = ColorThemer.COLOR_CARD and 0xffffff or 0x55000000
        window.statusBarColor = bc
        window.navigationBarColor = bc

        val dp = resources.displayMetrics.density

        val recycler = RecyclerView(this).apply {
            val p = (8 * dp).toInt()
            setPadding(0, 0, 0, p + Utils.getNavigationBarHeight(context))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        setContentView(recycler)

        val iconPacks = IconTheming.getAvailableIconPacks(packageManager).mapTo(LinkedList()) {
            IconPack(
                it.loadIcon(packageManager),
                it.loadLabel(packageManager).toString(),
                it.activityInfo.packageName
            )
        }

        iconPacks.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }

        val chosenIconPacks = run {
            val list = LinkedList<IconPack>()
            val strings = settings.getStrings("icon_packs")?.let(Array<String>::toMutableList) ?: return@run list
            var deleted = false
            for (string in strings) {
                val iconPack = iconPacks.find { it.packageName == string }
                if (iconPack == null) {
                    strings -= string
                    deleted = true
                } else {
                    iconPacks -= iconPack
                    list += iconPack
                }
            }
            if (deleted) {
                settings.edit(this) {
                    "icon_packs" set strings.toTypedArray()
                }
            }
            list
        }

        val a = packageManager.getApplicationInfo("android", 0)
        val systemPack = IconPack(
            a.loadIcon(packageManager),
            a.loadLabel(packageManager).toString(),
            "system"
        )

        val adapter = IconPackPickerAdapter(settings, chosenIconPacks, iconPacks, systemPack)
        recycler.adapter = adapter
        val th = ItemTouchHelper(TouchCallback(adapter))
        th.attachToRecyclerView(recycler)
    }

    class TouchCallback(private val adapter: IconPackPickerAdapter) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) = makeMovementFlags(if (viewHolder is IconPackViewHolder && viewHolder.type != IconPackPickerAdapter.SYSTEM_ICON_PACK) UP or DOWN else 0, 0)

        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = adapter.onItemMove(recyclerView.context, viewHolder, target)
    }

    class IconPack(
        val icon: Drawable,
        val label: String,
        val packageName: String,
    )
}