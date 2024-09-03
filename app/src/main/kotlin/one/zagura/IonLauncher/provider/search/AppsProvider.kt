package one.zagura.IonLauncher.provider.search

import android.content.Context
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.icons.LabelLoader

data object AppsProvider : BasicProvider<App> {

    private var list = emptyList<Pair<App, String>>()

    override fun updateData(context: Context) {
        list = AppLoader.getResource().map {
            it to LabelLoader.loadLabel(context, it)
        }
    }

    override fun clearData() {
        list = emptyList()
    }

    override fun getBaseData() = list

    override fun extraFactor(query: String, item: App, label: String): Float {
        val initialsFactor =
            if (query.length > 1 && SearchProvider.matchInitials(query, label)) 0.9f + query.length * 0.1f else 0f
        val r = FuzzySearch.tokenSortPartialRatio(query, item.packageName) / 100f
        return initialsFactor + r * r * r * 0.9f
    }
}