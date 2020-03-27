package personal.wuqing.rogue.utils

class DomTree<T : DirectionalNodeWithPrev<T>> private constructor(val root: T, val child: Map<T, List<T>>) {
    constructor(root: T) : this(root, Builder(root).build())

    class Builder<T : DirectionalNodeWithPrev<T>>(private val root: T) {
        private var cnt = 0
        private val dfn = mutableMapOf<T, Int>()
        private val vertex = mutableListOf<T>()
        private val parent = mutableMapOf<T, T>()
        private val semi = mutableMapOf<T, T>()
        private val ancestor = mutableMapOf<T, T>()
        private val sameDom = mutableMapOf<T, T>()
        private val idom = mutableMapOf<T, T>()
        private val best = mutableMapOf<T, T>()

        private val bucket = mutableMapOf<T, MutableList<T>>()

        private fun dfs(cur: T) {
            dfn[cur] = cnt++
            vertex += cur
            for (n in cur.next) if (n !in dfn) {
                dfs(n)
                parent[n] = cur
            }
        }

        private fun get(v: T): T {
            val a = ancestor[v]!!
            ancestor[a]?.let {
                val b = get(a)
                ancestor[v] = it
                if (dfn[semi[b]!!]!! < dfn[semi[best[v]!!]!!]!!) best[v] = b
            }
            return best[v]!!
        }

        private fun link(p: T, n: T) {
            ancestor[n] = p
            best[n] = n
        }

        private fun dominators() {
            dfs(root)
            for (i in cnt - 1 downTo 1) {
                val n = vertex[i]
                val p = parent[n]!!
                var s = p
                for (v in n.prev) {
                    val ss = if (dfn[v]!! <= dfn[n]!!) v else semi[get(v)]!!
                    if (dfn[ss]!! < dfn[s]!!) s = ss
                }
                semi[n] = s
                bucket.computeIfAbsent(s) { mutableListOf() } += n
                link(p, n)
                bucket[p]?.let {
                    for (v in it) {
                        val y = get(v)
                        if (semi[y] == semi[v]) idom[v] = p
                        else sameDom[v] = y
                    }
                    it.clear()
                }
            }
            for (i in 1 until cnt) vertex[i].let { n ->
                sameDom[n]?.let { idom[n] = idom[it]!! }
            }
        }

        fun build(): Map<T, List<T>> {
            dominators()
            val child = (vertex zip vertex.map { mutableListOf<T>() }).toMap()
            idom.forEach { (t, u) -> child[u]?.add(t) }
            return child
        }
    }
}
