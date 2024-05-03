package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.provider.items.ContactsLoader

object ContactsProvider : BasicProvider<ContactItem> {

    private var contacts = emptyList<ContactItem>()

    override fun updateData(context: Context) {
        contacts = ContactsLoader.load(context, false)
    }

    override fun clearData() {
        contacts = emptyList()
    }

    override fun getBaseData() = contacts
    override fun extraFactor(query: String, item: ContactItem) =
        if (item.isFavorite) 0.5f else 0f
}