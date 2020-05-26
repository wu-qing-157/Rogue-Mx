package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.utils.DirectionalNodeWithPrev

object FunctionInline {
    private const val LIMIT = 2048

    private class Node(val function: IRFunction.Declared) : DirectionalNodeWithPrev<Node> {
        override val next = mutableSetOf<Node>()
        override val prev = mutableSetOf<Node>()
        val size get() = function.body.sumBy { it.phi.size + it.normal.size }
    }

    private val nodeMap = mutableMapOf<IRFunction.Declared, Node>()
    private val left = mutableSetOf<Node>()

    private val simpleQueue = mutableSetOf<Node>()

    private fun clear() {
        nodeMap.clear()
        left.clear()
        simpleQueue.clear()
    }

    private var count = 0

    private fun trySimplifyFunctionPair(p: Node, n: Node) {
        val limit = (LIMIT - p.size) / n.size
        var times = 0
        val target = mutableMapOf<IRBlock, IRBlock>()
        val from = mutableMapOf<IRBlock, IRBlock>()
        val translatedBlocks = mutableListOf<IRBlock>()
        fun current() = translatedBlocks.last()
        for (block in p.function.body) {
            val new = IRBlock(block.name)
            target[block] = new
            translatedBlocks += new
            new.phi += block.phi
            for (state in block.normal)
                if (state is IRStatement.Normal.Call && state.function == n.function && ++times <= limit) {
                    val itemMap = n.function.body.map { b -> (b.phi + b.normal + b.terminate).map { it.all }.flatten() }
                        .flatten().associateWith { IRItem.Local() } + (n.function.args zip state.args)
                    val endBlock = IRBlock("inline.$count.end")
                    val newName = n.function.body.map {
                        it to IRBlock("inline.$count.${it.name}").apply {
                            phi += it.phi.map { st -> st.transform(itemMap) }
                            normal += it.normal.map { st -> st.transform(itemMap) }
                            terminate = it.terminate.transform(itemMap)
                            if (terminate is IRStatement.Terminate.Ret) terminate = IRStatement.Terminate.Jump(endBlock)
                        }
                    }.let {
                        current().terminate = IRStatement.Terminate.Jump(it[0].second)
                        it.toMap().also { map ->
                            translatedBlocks += it.map { (_, b) ->
                                b.apply {
                                    terminate = terminate.translate(map)
                                    phi.replaceAll { st -> st.translate(map) }
                                }
                            }
                        }
                    }
                    translatedBlocks += endBlock
                    if (state.result != null) {
                        endBlock.phi += IRStatement.Phi(
                            state.result,
                            n.function.body.filter { it.terminate is IRStatement.Terminate.Ret }.associate {
                                (newName[it] ?: error("cannot find block")) to
                                        ((it.terminate as IRStatement.Terminate.Ret).item?.let { item ->
                                            itemMap[item] ?: item
                                        } ?: IRItem.Const(0))
                            }
                        )
                    }
                    count++
                } else current().normal += state
            current().terminate = block.terminate
            from[block] = current()
        }
        p.function.body.clear()
        p.function.body += translatedBlocks.onEach {
            it.terminate = it.terminate.translate(target)
            it.phi.replaceAll { st -> st.translate(from).translate(target) }
        }
    }

    private fun trySimplifyCalleeFunction(node: Node) = node.prev.forEach { trySimplifyFunctionPair(it, node) }

    operator fun invoke(program: IRProgram) {
        clear()
        nodeMap += program.function.associateWith { Node(it).also { n -> left += n } }
        for (func in program.function) for (block in func.body) for (state in block.normal)
            if (state is IRStatement.Normal.Call && state.function is IRFunction.Declared) {
                val a = nodeMap[func] ?: error("cannot find function")
                val b = nodeMap[state.function] ?: error("cannot find function")
                a.next += b
                b.prev += a
            }
        for (n in nodeMap.values) if (n.next.isEmpty()) simpleQueue += n
        while (left.isNotEmpty()) {
            if (simpleQueue.isNotEmpty()) simpleQueue.minBy { it.size }?.let {
                simpleQueue -= it
                left -= it
                trySimplifyCalleeFunction(it)
                it.prev.forEach { p ->
                    p.next -= it
                    if (p.next.isEmpty()) simpleQueue += p
                }
                it.prev.clear()
            } else {
                val p = left.random()
                val n = p.next.random()
                p.next -= n
                n.prev -= p
                if (p.next.isEmpty()) simpleQueue += p
            }
        }
    }
}
