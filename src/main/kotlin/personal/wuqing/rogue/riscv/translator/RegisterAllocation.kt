package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.riscv.grammar.RVFunction
import personal.wuqing.rogue.riscv.grammar.RVInstruction
import personal.wuqing.rogue.riscv.grammar.RVRegister
import personal.wuqing.rogue.utils.BidirectionalEdge
import java.util.TreeSet

object RegisterAllocation {
    private val k = RVRegister.all.size

    private class Node(val register: RVRegister, val precolored: Boolean) {
        val adjacent = mutableSetOf<Node>()
        fun adjacent() = adjacent - select - coalescedNode
        var degree = 0
        fun decDegree() {
            if (degree-- == k && !precolored) {
                enableMoves(adjacent() + this)
                spillQueue -= this
                if (moveRelated()) freezeQueue += this
                else simplifyQueue += this
            }
        }

        val move = mutableSetOf<RVInstruction.Move>()
        fun move() = move.filter { it in active || it in moveQueue }
        fun moveRelated() = move.any { it in active || it in moveQueue }
        lateinit var color: RVRegister
        val colored get()= this::color.isInitialized

        override fun toString() = register.toString()
    }

    private val regMap = mutableMapOf<RVRegister, Node>()

    private class Edge(a: Node, b: Node) : BidirectionalEdge<Node>(a, b)

    private val alias = mutableMapOf<Node, Node>()
    private fun alias(n: Node): Node = alias[n].let { f ->
        if (f == null) n else alias(f).also { alias[n] = it }
    }

    private val conflict = mutableSetOf<Edge>()

    private val initial = mutableSetOf<Node>()
    private val spilled = mutableSetOf<Node>()
    private val coalescedNode = mutableSetOf<Node>()
    private val select = mutableSetOf<Node>()

    private val simplifyQueue = mutableSetOf<Node>()
    private val freezeQueue = mutableSetOf<Node>()
    private val spillQueue = mutableSetOf<Node>()

    private val moveQueue = mutableSetOf<RVInstruction.Move>()
    private val coalesced = mutableSetOf<RVInstruction.Move>()
    private val constrained = mutableSetOf<RVInstruction.Move>()
    private val frozen = mutableSetOf<RVInstruction.Move>()
    private val active = mutableSetOf<RVInstruction.Move>()

    private fun clear() {
        regMap.clear()
        alias.clear()
        conflict.clear()
        initial.clear()
        spilled.clear()
        coalescedNode.clear()
        select.clear()
        simplifyQueue.clear()
        freezeQueue.clear()
        spillQueue.clear()
        moveQueue.clear()
        coalesced.clear()
        constrained.clear()
        frozen.clear()
        active.clear()
    }

    private fun addEdge(u: Node, v: Node) {
        if (Edge(u, v) !in conflict) {
            conflict += Edge(u, v)
            u.adjacent += v
            u.degree++
            v.adjacent += u
            v.degree++
        }
    }

    private fun build(func: RVFunction) {
        clear()
        val liveness = Liveness(func)
        val registers = func.body.map { f -> f.instructions.map { it.use + it.def } }.flatten().flatten().toSet() +
                RVRegister.all
        val map = registers.associateWith {
            when (it) {
                is RVRegister.Virtual -> Node(it, false).also { n -> initial += n }
                is RVRegister.Spilled -> error("spilled register met when build conflict graph")
                else -> Node(it, true).also { n -> n.color = it }
            }
        }.also { regMap += it }
        for (u in RVRegister.all) for (v in RVRegister.all) if (u != v) {
            val aa = map[u] ?: error("cannot find register")
            val bb = map[v] ?: error("cannot find register")
            addEdge(aa, bb)
        }
        liveness.conflict.forEach { (a, b) ->
            val aa = map[a] ?: error("cannot find register")
            val bb = map[b] ?: error("cannot find register")
            addEdge(aa, bb)
        }
        liveness.coalesce.forEach {
            val aa = map[it.src] ?: error("cannot find register")
            val bb = map[it.dest] ?: error("cannot find register")
            moveQueue += it
            aa.move += it
            bb.move += it
        }
    }

    private fun initQueue() {
        for (n in initial) when {
            n.degree >= k -> spillQueue += n
            n.moveRelated() -> freezeQueue += n
            else -> simplifyQueue += n
        }
        initial.clear()
    }

    private fun addToQueue(node: Node) {
        if (!node.precolored && !node.moveRelated() && node.degree < k) {
            freezeQueue -= node
            simplifyQueue += node
        }
    }

    private fun conservative(nodes: Iterable<Node>) = nodes.count { it.degree >= k } < k

    private fun combine(u: Node, v: Node) {
        if (v in freezeQueue) freezeQueue -= v
        else spillQueue -= v
        coalescedNode += v
        alias[v] = u
        u.move += v.move
        enableMoves(listOf(v))
        for (t in v.adjacent()) {
            addEdge(t, u)
            t.decDegree()
        }
        if (u.degree >= k && u in freezeQueue) {
            freezeQueue -= u
            spillQueue += u
        }
    }

    private fun enableMoves(nodes: Iterable<Node>) {
        for (n in nodes) for (m in n.move()) if (m in active) {
            active -= m
            moveQueue += m
        }
    }

    private fun freezeMoves(u: Node) {
        for (m in u.move()) {
            val x = alias(regMap[m.src] ?: error("cannot find register"))
            val y = alias(regMap[m.dest] ?: error("cannot find register"))
            val v = if (alias(y) == alias(u)) alias(x) else alias(y)
            active -= m
            frozen += m
            if (!v.moveRelated() && v.degree < k) {
                freezeQueue -= v
                simplifyQueue += v
            }
        }
    }

    private fun simplify() {
        val n = simplifyQueue.first()
        simplifyQueue -= n
        select += n
        for (m in n.adjacent()) m.decDegree()
    }

    private fun coalesce() {
        val m = moveQueue.first()
        moveQueue -= m
        val x = alias(regMap[m.src] ?: error("cannot find register"))
        val y = alias(regMap[m.dest] ?: error("cannot find register"))
        val (u, v) = if (y.precolored) y to x else x to y
        when {
            u == v -> {
                coalesced += m
                addToQueue(u)
            }
            v.precolored || Edge(u, v) in conflict -> {
                constrained += m
                addToQueue(u)
                addToQueue(v)
            }
            if (u.precolored) v.adjacent().all { t -> t.degree < k || t.precolored || Edge(t, u) in conflict }
            else conservative(u.adjacent() + v.adjacent()) -> {
                coalesced += m
                combine(u, v)
                addToQueue(u)
            }
            else -> {
                active += m
            }
        }
    }

    private fun freeze() {
        val u = freezeQueue.first()
        freezeQueue -= u
        simplifyQueue += u
        freezeMoves(u)
    }

    private fun spill() {
        val m = spillQueue.maxBy { it.degree } ?: error("queue is empty")
        spillQueue -= m
        simplifyQueue += m
        freezeMoves(m)
    }

    private fun assignColors() {
        for (n in select.reversed()) {
            val ok = RVRegister.all.toMutableList()
            for (w in n.adjacent) alias(w).let { if (it.colored) ok -= it.color }
            if (ok.isEmpty()) spilled += n
            else n.color = ok.first()
        }
        for (n in coalescedNode) n.color = alias(n).color
    }

    private fun rewrite(func: RVFunction) {
        val map = spilled.map { it.register }.associateWith { RVRegister.Spilled(func.nextStack()) }
        for (block in func.body) block.instructions.replaceAll { it.transform(map) }
        Spiller(func)
    }

    private fun applyColor(func: RVFunction) {
        val map = regMap.values.map { it.register to it.color }.toMap()
        for (block in func.body) block.instructions.apply {
            removeAll(coalesced)
            replaceAll { it.transform(map) }
            removeAll { it is RVInstruction.Move && it.dest == it.src }
            if (func.size == 0) removeIf { it is RVInstruction.SPGrow || it is RVInstruction.SPRecover }
        }
    }

    operator fun invoke(func: RVFunction) {
        while (true) {
            clear()
            build(func)
            initQueue()
            working@ while (true) {
                when {
                    simplifyQueue.isNotEmpty() -> simplify()
                    moveQueue.isNotEmpty() -> coalesce()
                    freezeQueue.isNotEmpty() -> freeze()
                    spillQueue.isNotEmpty() -> spill()
                    else -> break@working
                }
            }
            assignColors()
            if (spilled.isNotEmpty()) rewrite(func)
            else break
        }
        applyColor(func)
    }
}
