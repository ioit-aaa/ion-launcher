package one.zagura.IonLauncher.provider.suggestions

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
            val matches = list.mapIndexedTo(ArrayList()) { ai, a ->
                a to list.mapIndexedTo(ArrayList()) { i, b ->
                    i to if (i == ai) 0f else calculateDistance(a, b)
                }
                .also { it.removeAt(ai) }
                .minBy { (_, c) -> c }
            }
            matches.sortBy { (_, closest) ->
                closest.second
            }
            var amountOfFiledMixAttempts = 0
            var iOffset = 0
            while (matches.size > maxContexts || amountOfFiledMixAttempts > matches.size) {
                val match = matches.removeAt(0)
                iOffset++
                val (matchData, matchLoc) = match
                if (matchLoc.first == -1) {
                    amountOfFiledMixAttempts++
                    matches.add(match)
                    continue
                }
                val trueI = matchLoc.first - iOffset
                val (arr, loc) = matches[trueI]
                arr.data.forEachIndexed { i, f ->
                    arr.data[i] = ((f.toInt() + matchData.data[i].toInt()) / 2).toShort()
                }
                matches[trueI] = arr to loc.copy(first = -1)
            }
            println("context map trim -> initial size: $s, new size: ${matches.size}")
            matches.map { it.first }
        } else list
    }

    fun push(item: T, data: ContextArray, maxContexts: Int) {
        contexts[item] = contexts[item]?.plus(data)?.let { trimContextListIfTooBig(it, maxContexts) } ?: listOf(data)
    }
}