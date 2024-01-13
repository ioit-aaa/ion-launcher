package one.zagura.IonLauncher.provider.search

import android.content.Context
import android.os.Build
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.provider.suggestions.SuggestionsManager

object Search {

    private val providers = listOfNotNull(
        AppsProvider,
        ContactsProvider,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            ShortcutsProvider
        else null,
        SettingsProvider,
    )

    fun updateData(context: Context) {
        for (p in providers)
            p.updateData(context)
    }

    fun clearData() {
        for (p in providers)
            p.clearData()
    }

    fun query(query: String): List<LauncherItem> {
        val suggestions = SuggestionsManager.getResource().let {
            it.subList(0, it.size.coerceAtMost(6))
        }
        val results = ArrayList<Pair<LauncherItem, Float>>()
        providers.flatMapTo(results) { it.query(query) }
        results.sortByDescending { (item, matchness) ->
            val i = suggestions.indexOf(item)
            val suggestionFactor = if (i == -1) 0f else (suggestions.size - i).toFloat() / suggestions.size
            matchness + suggestionFactor
        }
        return results.map { it.first }
    }
}