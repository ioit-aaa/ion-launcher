package one.zagura.IonLauncher.provider.search

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.ShortcutLoader

@RequiresApi(Build.VERSION_CODES.N_MR1)
object ShortcutsProvider : BasicProvider<StaticShortcut> {

    private var shortcuts = emptyList<StaticShortcut>()

    override fun updateData(context: Context) {
        shortcuts = AppLoader.getResource().flatMap { ShortcutLoader.getStaticShortcuts(context, it) }
    }

    override fun clearData() {
        shortcuts = emptyList()
    }

    override fun getBaseData() = shortcuts
    override fun extraFactor(query: String, item: StaticShortcut): Float {
        val initialsFactor = if (SearchProvider.matchInitials(query.substringBefore(' '), item.appLabel))
            0.7f else 0f
        val labelFactor = run {
            val r = FuzzySearch.tokenSortPartialRatio(query, item.appLabel) / 100f
            r * r * 0.8f
        }
        return initialsFactor + labelFactor
    }
}