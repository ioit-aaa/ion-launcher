package one.zagura.IonLauncher.provider.search

import android.content.Context
import android.os.Build
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.search.SearchProvider.Companion.removeDiacritics
import one.zagura.IonLauncher.util.Cancellable
import java.util.TreeSet

object Search {

    private val providers = listOfNotNull(
        AppsProvider,
        ContactsProvider,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            ShortcutsProvider
        else null,
        SettingsProvider,
        HiddenProvider,
    )

    fun updateData(context: Context) {
        for (p in providers)
            p.updateData(context)
    }

    fun clearData() {
        for (p in providers)
            p.clearData()
    }

    fun query(query: String, cancellable: Cancellable): List<LauncherItem> {
        val results = TreeSet<Pair<LauncherItem, Float>> { a, b ->
            when {
                a.first == b.first -> 0
                a.second > b.second -> -1
                else -> 1
            }
        }
        val q = query.trim().lowercase().removeDiacritics()
        for (provider in providers)
            if (cancellable.isCancelled) return emptyList()
            else provider.query(q, results, cancellable)
        return results.map { it.first }
    }
}