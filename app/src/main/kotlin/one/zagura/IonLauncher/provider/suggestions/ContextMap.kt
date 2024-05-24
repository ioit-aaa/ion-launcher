package one.zagura.IonLauncher.provider.suggestions

import java.util.ArrayList
import java.util.TreeSet

class ContextMap<T> : Map<T, List<ContextItem>> {

    private var contexts = HashMap<T, ArrayList<ContextItem>>()

    operator fun set(item: T, value: ArrayList<ContextItem>) {
        contexts[item] = value
    }

    override val entries: Set<Map.Entry<T, List<ContextItem>>>
        get() = contexts.entries
    override val keys: Set<T>
        get() = contexts.keys
    override val size: Int
        get() = contexts.size
    override val values: Collection<List<ContextItem>>
        get() = contexts.values

    override fun containsKey(key: T) = contexts.containsKey(key)
    override fun containsValue(value: List<ContextItem>) = contexts.containsValue(value)
    override fun get(key: T) = contexts[key]
    override fun isEmpty() = contexts.isEmpty()

    fun calculateDistance(currentContext: ContextItem, multipleContexts: List<ContextItem>): Float {
        if (multipleContexts.isEmpty())
            return Float.MAX_VALUE
        var a = 1f
        for (d in multipleContexts)
            a *= ContextItem.calculateDistance(currentContext, d)
        return a
    }

    fun push(item: T, data: ContextItem, maxContexts: Int) {
        val itemContexts = contexts[item]
        if (itemContexts == null) {
            contexts[item] = arrayListOf(data)
            return
        }
        itemContexts.add(data)
        val s = itemContexts.size
        if (s > maxContexts) {
            val matches = TreeSet<Pair<ContextItem, Pair<Int, Float>>> { (aa, a), (bb, b) ->
                when {
                    aa.data == bb.data -> 0
                    a.second < b.second -> -1
                    else -> 1
                }
            }
            itemContexts.mapIndexedTo(matches) { ai, a ->
                var closest = -1 to Float.MAX_VALUE
                for ((i, item) in itemContexts.withIndex()) {
                    if (i == ai)
                        continue
                    val d = ContextItem.calculateDistance(a, item)
                    if (d < closest.second)
                        closest = i to d
                }
                a to closest
            }
            val matchesList = matches.toMutableList()
            var amountOfFailedMixAttempts = 0
            var iOffset = 0
            while (matchesList.size > maxContexts && amountOfFailedMixAttempts < matchesList.size) {
                val match = matchesList.removeAt(0)
                iOffset++
                val (matchData, matchLoc) = match
                val trueI = matchLoc.first - iOffset
                if (trueI < 0) {
                    amountOfFailedMixAttempts++
                    matchesList.add(match)
                    continue
                }
                val (arr, loc) = matchesList[trueI]
                matchesList[trueI] = ContextItem.mix(arr, matchData) to loc.copy(first = -1)
            }
            itemContexts.clear()
            matchesList.mapTo(itemContexts) { it.first }
            itemContexts.trimToSize()
            println("context map trim -> initial size: $s, new size: ${itemContexts.size}")
        }
    }
}