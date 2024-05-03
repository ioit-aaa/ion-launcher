package one.zagura.IonLauncher.provider.search

import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.LauncherItem

interface BasicProvider<T : LauncherItem> : SearchProvider {

    fun getBaseData(): List<T>
    fun extraFactor(query: String, item: T): Float

    override fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>) {
        for (item in getBaseData()) {
            val extraFactor = extraFactor(query, item)
            val initialsFactor =
                if (query.length > 1 && SearchProvider.matchInitials(query, item.label)) 0.6f + query.length * 0.1f else 0f
            val labelFactor = FuzzySearch.tokenSortPartialRatio(query, item.label) / 100f
            val r = labelFactor +
                    initialsFactor +
                    extraFactor
            if (r > .8f)
                out.add(item to r)
        }
    }
}