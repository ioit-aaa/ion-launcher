package one.zagura.IonLauncher.provider.search

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.data.items.StaticShortcut
import one.zagura.IonLauncher.provider.items.AppLoader
import one.zagura.IonLauncher.provider.items.LabelLoader
import one.zagura.IonLauncher.provider.items.ShortcutLoader

@RequiresApi(Build.VERSION_CODES.N_MR1)
object ShortcutsProvider : SearchProvider {

    private var shortcuts = emptyList<Triple<StaticShortcut, String, String>>()

    override fun updateData(context: Context) {
        val s = ArrayList<StaticShortcut>()
        AppLoader.getResource().forEach { ShortcutLoader.getStaticShortcuts(context, it, s) }
//        s.trimToSize()
        shortcuts = s.map { Triple(it, LabelLoader.loadLabel(context, it), LabelLoader.loadLabel(context, it.packageName, it.userHandle)) }
    }

    override fun clearData() {
        shortcuts = emptyList()
    }

    fun extraFactor(query: String, item: StaticShortcut, label: String, appLabel: String): Float {
        val initialsFactor = if (SearchProvider.matchInitials(query.substringBefore(' '), appLabel))
            0.7f else 0f
        val labelFactor = run {
            val r = FuzzySearch.tokenSortPartialRatio(query, appLabel) / 100f
            r * r * 0.8f
        }
        return initialsFactor + labelFactor
    }

    override fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>) {
        for ((item, label, appLabel) in shortcuts) {
            val extraFactor = extraFactor(query, item, label, appLabel)
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