package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.DEBUG
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.riscv.RVPrinter
import personal.wuqing.rogue.riscv.grammar.RVProgram
import java.io.FileWriter

object RVTranslator {
    var outputCount = 0

    private fun debug(rv: RVProgram, description: String) {
        if (DEBUG) FileWriter("debug/ASM${outputCount++}.s").use {
            it.write("# Current Step: $description\n\n")
            it.write(RVPrinter(rv))
        }
    }

    operator fun invoke(program: IRProgram): RVProgram {
        val rv = IR2RVTranslator(program)
        debug(rv, "IR2RV")

        rv.function.forEach { Spiller(it) }
        debug(rv, "Spiller")

        rv.function.forEach { RegisterAllocation(it) }
        debug(rv, "Register Allocation")

        return rv
    }
}
