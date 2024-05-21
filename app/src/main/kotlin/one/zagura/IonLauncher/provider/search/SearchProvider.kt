package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem

sealed interface SearchProvider {

    fun updateData(context: Context) {}
    fun clearData() {}

    fun query(query: String, out: MutableCollection<Pair<LauncherItem, Float>>)

    companion object {
        /**
         * @return true if [string]'s initials contain [query]
         */
        fun matchInitials(query: String, string: String): Boolean {
            var i = 0
            while (i < string.length && string[i] in " .\\-_")
                i++
            for (c in query) {
                if (i == string.length)
                    return false
                if (string[i].lowercaseChar() != c.lowercaseChar())
                    return false
                while (true) {
                    if (++i == string.length || string[i] in " .\\-_")
                        break
                    if (i < string.lastIndex && string[i].isLowerCase() && string[i + 1].isUpperCase())
                        break
                }
            }
            return true
        }
    }
}