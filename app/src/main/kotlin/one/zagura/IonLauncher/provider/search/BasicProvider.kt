package one.zagura.IonLauncher.provider.search

import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.LauncherItem

sealed interface BasicProvider<T : LauncherItem> : SearchProvider {

    fun getBaseData(): List<Pair<T, String>>
    fun extraFactor(query: String, item: T, label: String): Float

    override fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>) {
        for ((item, label) in getBaseData()) {
            val extraFactor = extraFactor(query, item, label)
            val initialsFactor =
                if (query.length > 1 && SearchProvider.matchInitials(query, label)) 0.6f + query.length * 0.1f else 0f
            val labelFactor = FuzzySearch.tokenSortPartialRatio(query, label) / 100f
            val r = labelFactor +
                    initialsFactor +
                    extraFactor
            if (r > .8f)
                out.add(item to r)
        }
    }
}