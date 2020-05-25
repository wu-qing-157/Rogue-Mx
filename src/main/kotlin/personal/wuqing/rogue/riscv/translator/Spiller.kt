package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.riscv.grammar.RVFunction
import personal.wuqing.rogue.riscv.grammar.RVInstruction
import personal.wuqing.rogue.riscv.grammar.RVRegister

object Spiller {
    operator fun invoke(function: RVFunction) {
        for (block in function.body) {
            val result = block.instructions.map {
                val map = mutableMapOf<RVRegister.Spilled, RVRegister>()
                if (it is RVInstruction.Move) {
                    when {
                        it.dest is RVRegister.Spilled && it.src is RVRegister.Spilled ->
                            RVRegister.Virtual().let { reg ->
                                listOf(
                                    RVInstruction.Load(reg, it.src.address), RVInstruction.Save(reg, it.dest.address)
                                )
                            }
                        it.dest is RVRegister.Spilled -> listOf(RVInstruction.Save(it.src, it.dest.address))
                        it.src is RVRegister.Spilled -> listOf(RVInstruction.Load(it.dest, it.src.address))
                        else -> listOf(it)
                    }
                } else {
                    val prec = it.use.filterIsInstance<RVRegister.Spilled>().map {
                        RVRegister.Virtual().let { reg ->
                            map[it] = reg
                            RVInstruction.Load(reg, it.address)
                        }
                    }
                    val succ = it.def.filterIsInstance<RVRegister.Spilled>().map {
                        RVRegister.Virtual().let { reg ->
                            map[it] = reg
                            RVInstruction.Save(reg, it.address)
                        }
                    }
                    prec + it.transform(map) + succ
                }
            }.flatten()
            block.instructions.clear()
            block.instructions.addAll(result)
        }
    }
}
