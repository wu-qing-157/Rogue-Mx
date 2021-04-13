package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object JumpSimplifier {
    private fun mergeSimpleJump(function: IRFunction.Declared) {
        val merged = mutableSetOf<IRBlock>()
        val trans = mutableMapOf<IRBlock, IRBlock>()
        val phiTrans = mutableMapOf<IRItem, IRItem>()

        for (block in function.body) {
            if (block in merged) continue
            var last = block
            while (true) {
                val next = block.next.singleOrNull() ?: break
                if (next.prev.singleOrNull()?.equals(last) == true) {
                    block.normal += next.normal
                    block.terminate = next.terminate
                    next.phi.forEach { phiTrans[it.result] = it.list[last] ?: error("Cannot find block in phi") }
                    merged += next
                    last = next
                    trans[next] = block
                } else break
            }
        }
        function.body -= merged

        val finalTrans = trans.keys.associateWith {
            var ret = it
            while (true) ret = trans[ret] ?: break
            ret
        }
        val finalPhiTrans = phiTrans.keys.associateWith {
            var ret = it
            while (true) ret = phiTrans[ret] ?: break
            ret
        }
        for (block in function.body) {
            block.phi.replaceAll { it.translate(finalTrans).transUse(finalPhiTrans) }
            block.normal.replaceAll { it.transUse(finalPhiTrans) }
            block.terminate = block.terminate.transUse(finalPhiTrans)
        }
        function.updatePrev()
    }

    private fun eliminatePhiOnly(function: IRFunction.Declared) {
        val eliminated = mutableSetOf<IRBlock>()
        for (block in function.body.subList(1, function.body.size))
            if (block.normal.isEmpty()) {
                val terminate = block.terminate
                if (terminate is IRStatement.Terminate.Jump && terminate.dest.prev.intersect(block.prev).isEmpty()) {
                    val phiMap = block.phi.associate { it.result to it.list }
                    val used = mutableSetOf<IRItem.Local>()
                    terminate.dest.phi.replaceAll { phi ->
                        val mapped = phi.list[block]!!
                        phiMap[mapped]?.let {
                            used += mapped as IRItem.Local
                            IRStatement.Phi(phi.result, phi.list - block + it)
                        } ?: IRStatement.Phi(phi.result, phi.list - block + block.prev.associateWith { mapped })
                    }
                    terminate.dest.phi += (phiMap - used).map { (r, list) ->
                        IRStatement.Phi(r, terminate.dest.prev.associateWith { r } - block + list)
                    }
                    block.prev.forEach { it.terminate = it.terminate.translate(mapOf(block to terminate.dest)) }
                    terminate.dest.prev += block.prev
                    terminate.dest.prev -= block
                    block.prev.clear()
                    eliminated += block
                }
                if (terminate is IRStatement.Terminate.Branch) {
                    for ((prev, item) in block.phi.firstOrNull { it.result == terminate.cond }?.list?.takeIf {
                        it.all { (prev, item) ->
                            when (item) {
                                IRItem.Const(1) -> prev !in terminate.then.prev
                                IRItem.Const(0) -> prev !in terminate.els.prev
                                else -> prev.terminate is IRStatement.Terminate.Jump && prev !in terminate.then.prev && prev !in terminate.els.prev
                            }
                        }
                    } ?: continue) when {
                        item == IRItem.Const(1) && prev !in terminate.then.prev -> {
                            prev.terminate = prev.terminate.translate(mapOf(block to terminate.then))
                            terminate.then.prev += prev
                        }
                        item == IRItem.Const(0) && prev !in terminate.els.prev -> {
                            prev.terminate = prev.terminate.translate(mapOf(block to terminate.els))
                            terminate.els.prev += prev
                        }
                        prev.terminate is IRStatement.Terminate.Jump -> {
                            prev.terminate = IRStatement.Terminate.Branch(item, terminate.then, terminate.els)
                            terminate.then.prev += prev
                            terminate.els.prev += prev
                        }
                    }
                    val phiMap = block.phi.associate { it.result to it.list }
                    val used = mutableSetOf<IRItem.Local>()
                    terminate.then.phi.replaceAll { phi ->
                        val mapped = phi.list[block]!!
                        phiMap[mapped]?.let {
                            used += mapped as IRItem.Local
                            IRStatement.Phi(phi.result, phi.list - block + it)
                        } ?: IRStatement.Phi(phi.result, phi.list - block + block.prev.associateWith { mapped })
                    }
                    terminate.then.phi += (phiMap - used).map { (r, list) ->
                        IRStatement.Phi(r, terminate.then.prev.associateWith { r } - block + list)
                    }
                    used.clear()
                    terminate.els.phi.replaceAll { phi ->
                        val mapped = phi.list[block]!!
                        phiMap[mapped]?.let {
                            used += mapped as IRItem.Local
                            IRStatement.Phi(phi.result, phi.list - block + it)
                        } ?: IRStatement.Phi(phi.result, phi.list - block + block.prev.associateWith { mapped })
                    }
                    terminate.els.phi += (phiMap - used).map { (r, list) ->
                        IRStatement.Phi(r, terminate.els.prev.associateWith { r } - block + list)
                    }
                    terminate.then.prev -= block
                    terminate.els.prev -= block
                    eliminated += block
                }
            }
        function.body -= eliminated
    }

    operator fun invoke(ir: IRProgram) = ir.function.forEach {
        mergeSimpleJump(it)
        eliminatePhiOnly(it)
    }
}
