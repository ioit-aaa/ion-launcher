package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.icons.LabelLoader

object HiddenProvider : BasicProvider<LauncherItem> {

    private var hidden = emptyList<Pair<LauncherItem, String>>()

    override fun updateData(context: Context) {
        hidden = HiddenApps.getItems(context).map {
            it to LabelLoader.loadLabel(context, it).lowercase()
        }
    }

    override fun clearData() {
        hidden = emptyList()
    }

    override fun getBaseData() = hidden
    override fun extraFactor(query: String, item: LauncherItem, label: String) = -.1f
}