package one.zagura.IonLauncher.provider.suggestions

import java.util.ArrayList
import java.util.TreeSet

class ContextMap<T> : Map<T, List<ContextArray>> {

    private var contexts = HashMap<T, ArrayList<ContextArray>>()

    operator fun set(item: T, value: ArrayList<ContextArray>) {
        contexts[item] = value
    }

    override val entries: Set<Map.Entry<T, List<ContextArray>>>
        get() = contexts.entries
    override val keys: Set<T>
        get() = contexts.keys
    override val size: Int
        get() = contexts.size
    override val values: Collection<List<ContextArray>>
        get() = contexts.values

    override fun containsKey(key: T) = contexts.containsKey(key)
    override fun containsValue(value: List<ContextArray>) = contexts.containsValue(value)
    override fun get(key: T) = contexts[key]
    override fun isEmpty() = contexts.isEmpty()

    fun calculateDistance(currentContext: ContextArray, multipleContexts: List<ContextArray>): Float {
        if (multipleContexts.isEmpty())
            return Float.MAX_VALUE
        var a = 1f
        for (d in multipleContexts)
            a *= calculateDistance(currentContext, d)
        return a
    }

    private fun calculateDistance(a: ContextArray, b: ContextArray): Float {
        var sum = 0f
        a.data.forEachIndexed { i, fl ->
            sum += ContextArray.differentiator(i, fl, b.data[i])
        }
        return sum / a.data.size
    }

    fun push(item: T, data: ContextArray, maxContexts: Int) {
        val itemContexts = contexts[item]
        if (itemContexts == null) {
            contexts[item] = arrayListOf(data)
            return
        }
        itemContexts.add(data)
        val s = itemContexts.size
        if (s > maxContexts) {
            val matches = TreeSet<Pair<ContextArray, Pair<Int, Float>>> { (aa, a), (bb, b) ->
                when {
                    aa.data === bb.data -> 0
                    a.second < b.second -> -1
                    else -> 1
                }
            }
            itemContexts.mapIndexedTo(matches) { ai, a ->
                var closest = -1 to Float.MAX_VALUE
                for ((i, item) in itemContexts.withIndex()) {
                    if (i == ai)
                        continue
                    val d = calculateDistance(a, item)
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
                arr.data.forEachIndexed { i, f ->
                    arr.data[i] = ((f.toInt() + matchData.data[i].toInt()) / 2).toShort()
                }
                matchesList[trueI] = arr to loc.copy(first = -1)
            }
            itemContexts.clear()
            matchesList.mapTo(itemContexts) { it.first }
            itemContexts.trimToSize()
            println("context map trim -> initial size: $s, new size: ${itemContexts.size}")
        }
    }
}