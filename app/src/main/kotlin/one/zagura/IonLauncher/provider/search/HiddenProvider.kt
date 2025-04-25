package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.icons.LabelLoader

data object HiddenProvider : BasicProvider<LauncherItem> {

    private var hidden = ArrayList<Pair<LauncherItem, String>>()

    override fun updateData(context: Context) {
        hidden.clear()
        HiddenApps.getItems(context) {
            hidden.add(it to LabelLoader.loadLabel(context, it).lowercase())
        }
    }

    override fun clearData() {
        hidden.clear()
    }

    override fun getBaseData() = hidden
    override fun extraFactor(query: String, item: LauncherItem, label: String) = -.1f
}