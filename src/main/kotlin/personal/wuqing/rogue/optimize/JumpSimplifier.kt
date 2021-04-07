package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram

object JumpSimplifier {
    private operator fun invoke(function: IRFunction.Declared) {
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

    operator fun invoke(ir: IRProgram) = ir.function.forEach { this(it) }
}
