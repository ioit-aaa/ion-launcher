package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.HiddenApps
import one.zagura.IonLauncher.provider.items.ContactsLoader

object HiddenProvider : BasicProvider<LauncherItem> {

    private var hidden = emptyList<LauncherItem>()

    override fun updateData(context: Context) {
        hidden = HiddenApps.getItems(context)
    }

    override fun clearData() {
        hidden = emptyList()
    }

    override fun getBaseData() = hidden
    override fun extraFactor(query: String, item: LauncherItem) = -.1f
}