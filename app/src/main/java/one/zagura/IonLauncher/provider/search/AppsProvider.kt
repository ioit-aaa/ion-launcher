package one.zagura.IonLauncher.provider.search

import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.App
import one.zagura.IonLauncher.provider.items.AppLoader

data object AppsProvider : BasicProvider<App> {
    override fun getBaseData() = AppLoader.getResource()
    override fun extraFactor(query: String, item: App) = run {
        val r = FuzzySearch.tokenSortPartialRatio(query, item.packageName) / 100f
        r * r * r * 0.9f
    }
}