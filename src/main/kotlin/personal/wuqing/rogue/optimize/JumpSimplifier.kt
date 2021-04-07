package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRProgram

object JumpSimplifier {
    private operator fun invoke(function: IRFunction.Declared) {
        val merged = mutableSetOf<IRBlock>()
        val trans = mutableMapOf<IRBlock, IRBlock>()

        for (block in function.body) {
            if (block in merged) continue
            var last = block
            while (true) {
                val next = block.next.singleOrNull() ?: break
                if (next.prev.singleOrNull()?.equals(last) == true) {
                    block.normal += next.normal
                    block.terminate = next.terminate
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
        for (block in function.body) block.phi.replaceAll { it.translate(finalTrans) }
        function.updatePrev()
    }

    operator fun invoke(ir: IRProgram) = ir.function.forEach { this(it) }
}
