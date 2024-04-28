package one.zagura.IonLauncher.provider.suggestions

import java.util.TreeSet

class ContextMap<T>(
    val differentiator: (Int, Short, Short) -> Float
) : Map<T, List<ContextArray>> {

    private var contexts = HashMap<T, List<ContextArray>>()

    operator fun set(item: T, value: List<ContextArray>) {
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
        return multipleContexts.map { d ->
            calculateDistance(currentContext, d)
        }.reduce(Float::times)
    }

    private fun calculateDistance(a: ContextArray, b: ContextArray): Float {
        var sum = 0f
        a.data.forEachIndexed { i, fl ->
            sum = differentiator(i, fl, b.data[i])
        }
        return sum
    }

    private fun trimContextListIfTooBig(list: List<ContextArray>, maxContexts: Int): List<ContextArray> {
        val s = list.size
        return if (list.size > maxContexts) {
            val matches = TreeSet<Pair<ContextArray, Pair<Int, Float>>> { (aa, a), (bb, b) ->
                (a.second * 1000f + (aa.hour / 24f + aa.dayOfYear) / 365f).compareTo(b.second * 1000f + (bb.hour / 24f + bb.dayOfYear) / 365f)
            }
            list.mapIndexedTo(matches) { ai, a ->
                var closest = -1 to Float.MAX_VALUE
                for ((i, item) in list.withIndex()) {
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
            println("context map trim -> initial size: $s, new size: ${matchesList.size}")
            matchesList.map { it.first }
        } else list
    }

    fun push(item: T, data: ContextArray, maxContexts: Int) {
        contexts[item] = contexts[item]?.plus(data)?.let { trimContextListIfTooBig(it, maxContexts) } ?: listOf(data)
    }
}