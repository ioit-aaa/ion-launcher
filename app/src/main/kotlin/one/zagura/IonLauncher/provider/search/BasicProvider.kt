package one.zagura.IonLauncher.provider.search

import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.util.Cancellable

sealed interface BasicProvider<T : LauncherItem> : SearchProvider {

    fun getBaseData(): List<Pair<T, String>>
    fun extraFactor(query: String, item: T, label: String): Float

    override fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>, cancellable: Cancellable) {
        for ((item, label) in getBaseData()) {
            if (cancellable.isCancelled)
                break
            val extraFactor = extraFactor(query, item, label)
            val labelFactor = FuzzySearch.tokenSortPartialRatio(query, label) / 100f
            val r = labelFactor + extraFactor
            if (r > .8f)
                out.add(item to r)
        }
    }
}