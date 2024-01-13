package one.zagura.IonLauncher.provider.search

import android.content.Context
import one.zagura.IonLauncher.data.items.LauncherItem

sealed interface SearchProvider {

    fun updateData(context: Context) {}
    fun clearData() {}

    fun query(query: String): List<Pair<LauncherItem, Float>>

    companion object {
        /**
         * @return true if [string]'s initials contain [query]
         */
        fun matchInitials(query: String, string: String): Boolean {
            val initials = string.split(Regex("([ .\\-_]|([a-z](?=[A-Z0-9])))")).mapNotNull(String::firstOrNull).joinToString("")
            val initialsBasic = string.split(Regex("[ .\\-_]")).mapNotNull(String::firstOrNull).joinToString("")
            return initials.contains(query, ignoreCase = true) || initialsBasic.contains(query, ignoreCase = true)
        }
    }
}