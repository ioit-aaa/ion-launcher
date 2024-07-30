package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem
import one.zagura.IonLauncher.util.Cancellable
import java.text.Normalizer

sealed interface SearchProvider {

    fun updateData(context: Context) {}
    fun clearData() {}

    fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>, cancellable: Cancellable)

    companion object {
        fun CharSequence.removeDiacritics(): String {
            return Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }

        /**
         * @return true if [string]'s initials contain [query]
         */
        fun matchInitials(query: String, string: String): Boolean {
            var i = 0
            for (c in query) {
                while (i < string.length && string[i] in " .\\/:-_")
                    i++
                if (i == string.length)
                    return false
                if (string[i].lowercaseChar() != c.lowercaseChar())
                    return false
                while (true) {
                    if (++i == string.length || string[i] in " .\\/:-_")
                        break
                    if (i < string.lastIndex && string[i - 1].isLowerCase() && string[i].isUpperCase())
                        break
                }
            }
            return true
        }
    }
}