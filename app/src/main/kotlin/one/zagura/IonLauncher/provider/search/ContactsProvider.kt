package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.ContactItem
import one.zagura.IonLauncher.provider.items.ContactsLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader
import one.zagura.IonLauncher.provider.search.SearchProvider.Companion.removeDiacritics

data object ContactsProvider : BasicProvider<ContactItem> {

    private var contacts = emptyList<Pair<ContactItem, String>>()

    override fun updateData(context: Context) {
        contacts = ContactsLoader.load(context, false).map {
            it to LabelLoader.loadLabel(context, it)
                .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}]"), "")
                .trim()
                .removeDiacritics()
        }
    }

    override fun clearData() {
        contacts = emptyList()
    }

    override fun getBaseData() = contacts
    override fun extraFactor(query: String, item: ContactItem, label: String): Float {
        val initialsFactor =
            if (query.length > 1 && SearchProvider.matchInitials(query, label)) 0.9f + query.length * 0.4f else 0f
        return initialsFactor + if (item.isFavorite) 0.2f else 0.0f
    }
}