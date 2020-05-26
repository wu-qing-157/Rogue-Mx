package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.riscv.grammar.RVAddress
import personal.wuqing.rogue.riscv.grammar.RVBlock
import personal.wuqing.rogue.riscv.grammar.RVFunction
import personal.wuqing.rogue.riscv.grammar.RVGlobal
import personal.wuqing.rogue.riscv.grammar.RVInstruction
import personal.wuqing.rogue.riscv.grammar.RVProgram
import personal.wuqing.rogue.riscv.grammar.RVRegister

object FinalFix {
    class Value

    private fun fixMove(block: RVBlock) {
        val value = mutableMapOf<RVRegister, Value>()
        val useless = mutableSetOf<RVInstruction>()
        for (inst in block.instructions)
            if (inst is RVInstruction.Move) {
                if (inst.dest == inst.src) {
                    useless += inst
                    continue
                }
                val a = value.computeIfAbsent(inst.dest) { Value() }
                val b = value.computeIfAbsent(inst.src) { Value() }
                if (a == b) useless += inst
                value[inst.dest] = b
            } else {
                for (reg in inst.def) value[reg] = Value()
            }
        block.instructions -= useless
    }

    private fun fixMemory(block: RVBlock) {
        val address = mutableMapOf<RVRegister, RVAddress>()
        val global = mutableMapOf<RVRegister, RVGlobal>()
        val changed = mutableMapOf<RVInstruction, RVInstruction>()
        val saveAddress = mutableMapOf<RVAddress, RVInstruction>()
        val saveGlobal = mutableMapOf<RVGlobal, RVInstruction>()
        val loaded = mutableSetOf<RVInstruction>()
        val covered = mutableSetOf<RVInstruction>()
        for (inst in block.instructions) {
            when (inst) {
                is RVInstruction.Load -> {
                    address.entries.firstOrNull { it.value == inst.addr }?.let {
                        changed += inst to RVInstruction.Move(inst.reg, it.key)
                    } ?: saveAddress[inst.addr]?.let { loaded += it }
                    address[inst.reg] = inst.addr
                }
                is RVInstruction.Save -> {
                    address.clear()
                    address[inst.reg] = inst.addr
                    saveAddress[inst.addr]?.let { if (it !in loaded) covered += it }
                    saveAddress[inst.addr] = inst
                }
                is RVInstruction.LG -> {
                    global.entries.firstOrNull { it.value == inst.global }?.let {
                        changed += inst to RVInstruction.Move(inst.reg, it.key)
                    } ?: saveGlobal[inst.global]?.let { loaded += it }
                    global[inst.reg] = inst.global
                }
                is RVInstruction.SG -> {
                    for ((reg, g) in global.toList()) if (g == inst.global) global -= reg
                    global[inst.reg] = inst.global
                    saveGlobal[inst.global]?.let { if (it !in loaded) covered += it }
                    saveGlobal[inst.global] = inst
                }
                is RVInstruction.Call -> {
                    address.clear()
                    global.clear()
                    saveGlobal.values.forEach { loaded += it }
                    saveAddress.values.forEach { loaded += it }
                }
                else -> Unit
            }
            address -= inst.def
            global -= inst.def
            for ((k, addr) in address.toList()) if (addr.base in inst.def) address -= k
            for (addr in saveAddress.keys.toList()) if (addr.base in inst.def) saveAddress -= addr
        }
        block.instructions.replaceAll { changed[it] ?: it }
        block.instructions -= covered
    }

    private fun fix(func: RVFunction) {
        func.body.forEach(::fixMemory)
        func.body.forEach(::fixMove)
        while (true) {
            var changed = false
            for ((prev, succ) in func.body.windowed(2)) prev.instructions.lastOrNull()?.let {
                if (it is RVInstruction.J && it.dest == succ) {
                    prev.instructions.removeAt(prev.instructions.size - 1)
                    changed = true
                }
            }
            val newTarget = mutableMapOf<RVBlock, RVBlock>()
            for ((prev, succ) in func.body.windowed(2)) if (prev.instructions.isEmpty())
                newTarget[succ] = newTarget[prev] ?: prev
            for (block in func.body) block.instructions.replaceAll {
                when (it) {
                    is RVInstruction.J -> RVInstruction.J(newTarget[it.dest] ?: it.dest)
                    is RVInstruction.Branch -> RVInstruction.Branch(
                        it.op, it.lhs, it.rhs, newTarget[it.dest] ?: it.dest
                    )
                    else -> it
                }
            }
            if (!changed) break
        }
    }

    operator fun invoke(program: RVProgram) = program.function.forEach(::fix)
}
