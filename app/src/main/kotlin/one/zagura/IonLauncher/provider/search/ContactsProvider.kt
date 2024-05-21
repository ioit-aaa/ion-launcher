package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.provider.items.ContactsLoader
import one.zagura.IonLauncher.provider.items.LabelLoader

object ContactsProvider : BasicProvider<ContactItem> {

    private var contacts = emptyList<Pair<ContactItem, String>>()

    override fun updateData(context: Context) {
        contacts = ContactsLoader.load(context, false).map {
            it to LabelLoader.loadLabel(context, it)
        }
    }

    override fun clearData() {
        contacts = emptyList()
    }

    override fun getBaseData() = contacts
    override fun extraFactor(query: String, item: ContactItem, label: String) =
        if (item.isFavorite) 0.5f else 0f
}