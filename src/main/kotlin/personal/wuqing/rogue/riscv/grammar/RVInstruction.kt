package personal.wuqing.rogue.riscv.grammar

import personal.wuqing.rogue.riscv.grammar.RVRegister.Companion.arg
import personal.wuqing.rogue.riscv.grammar.RVRegister.Companion.temp

sealed class RVInstruction {
    abstract val use: Iterable<RVRegister>
    abstract val def: Iterable<RVRegister>

    abstract fun transform(map: Map<out RVRegister, RVRegister>): RVInstruction

    class LI(val reg: RVRegister, val imm: Int) : RVInstruction() {
        override fun toString() = "\tli\t$reg, $imm"
        override val use = listOf<RVRegister>()
        override val def = listOf(reg)
        override fun transform(map: Map<out RVRegister, RVRegister>) = LI(map[reg] ?: reg, imm)
    }

    class LA(val reg: RVRegister, val literal: RVLiteral) : RVInstruction() {
        override fun toString() = "\tla\t$reg, $literal"
        override val use = listOf<RVRegister>()
        override val def = listOf(reg)
        override fun transform(map: Map<out RVRegister, RVRegister>) = LA(map[reg] ?: reg, literal)
    }

    class Load(val reg: RVRegister, val addr: RVAddress) : RVInstruction() {
        override fun toString() = "\tlw\t$reg, $addr"
        override val use = listOf(addr.base)
        override val def = listOf(reg)
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            Load(
                map[reg] ?: reg, when (addr) {
                    is RVAddress.Stack, is RVAddress.Pass, is RVAddress.Caller -> addr
                    else -> RVAddress(map[addr.base] ?: addr.base, addr.delta)
                }
            )
    }

    class LG(val reg: RVRegister, val global: RVGlobal) : RVInstruction() {
        override fun toString() = "\tlw\t$reg, $global"
        override val use = listOf<RVRegister>()
        override val def = listOf(reg)
        override fun transform(map: Map<out RVRegister, RVRegister>) = LG(map[reg] ?: reg, global)
    }

    class Save(val reg: RVRegister, val addr: RVAddress) : RVInstruction() {
        override fun toString() = "\tsw\t$reg, $addr"
        override val use = listOf(reg, addr.base)
        override val def = listOf<RVRegister>()
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            Save(
                map[reg] ?: reg, when (addr) {
                    is RVAddress.Stack, is RVAddress.Pass, is RVAddress.Caller -> addr
                    else -> RVAddress(map[addr.base] ?: addr.base, addr.delta)
                }
            )
    }

    class SG(val reg: RVRegister, val assist: RVRegister, val global: RVGlobal) : RVInstruction() {
        override fun toString() = "\tsw\t$reg, $global, $assist"
        override val use = listOf(reg, assist)
        override val def = listOf<RVRegister>()
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            SG(map[reg] ?: reg, map[assist] ?: assist, global)
    }

    class Move(val dest: RVRegister, val src: RVRegister) : RVInstruction() {
        override fun toString() = "\tmv\t$dest, $src"
        override val use = listOf(src)
        override val def = listOf(dest)
        override fun transform(map: Map<out RVRegister, RVRegister>) = Move(map[dest] ?: dest, map[src] ?: src)
    }

    class Calc(val op: RVCalcOp, val lhs: RVRegister, val rhs: RVRegister, val result: RVRegister) : RVInstruction() {
        override fun toString() = "\t${op.binary}\t$result, $lhs, $rhs"
        override val use = listOf(lhs, rhs)
        override val def = listOf(result)
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            Calc(op, map[lhs] ?: lhs, map[rhs] ?: rhs, map[result] ?: result)
    }

    class CalcI(val op: RVCalcOp, val lhs: RVRegister, val imm: Int, val result: RVRegister) : RVInstruction() {
        init {
            op.imm ?: error("invalid operator with immediate")
        }

        override fun toString() = "\t" + "${op.imm}\t$result, $lhs, $imm"
        override val use = listOf(lhs)
        override val def = listOf(result)
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            CalcI(op, map[lhs] ?: lhs, imm, map[result] ?: result)
    }

    class CmpZ(val op: RVCmpOp, val reg: RVRegister, val result: RVRegister) : RVInstruction() {
        init {
            op.zero ?: error("invalid operator comparing with zero")
        }

        override fun toString() = "\t${op.zero}\t$result, $reg"
        override val use = listOf(reg)
        override val def = listOf(result)
        override fun transform(map: Map<out RVRegister, RVRegister>) = CmpZ(op, map[reg] ?: reg, map[result] ?: result)
    }

    class J(val dest: RVBlock) : RVInstruction() {
        override fun toString() = "\tj\t$dest"
        override val use = listOf<RVRegister>()
        override val def = listOf<RVRegister>()
        override fun transform(map: Map<out RVRegister, RVRegister>) = this
    }

    class Ret : RVInstruction() {
        override fun toString() = "\tret"
        override val use = listOf(RVRegister.RA)
        override val def = listOf<RVRegister>()
        override fun transform(map: Map<out RVRegister, RVRegister>) = this
    }

    class Branch(val op: RVCmpOp, val lhs: RVRegister, val rhs: RVRegister, val dest: RVBlock) : RVInstruction() {
        override fun toString() = "\t${op.branch}\t$lhs, $rhs, $dest"
        override val use = listOf(lhs, rhs)
        override val def = listOf<RVRegister>()
        override fun transform(map: Map<out RVRegister, RVRegister>) =
            Branch(op, map[lhs] ?: lhs, map[rhs] ?: rhs, dest)
    }

    class Call(val symbol: String, val cnt: Int) : RVInstruction() {
        override fun toString() = "\tcall\t$symbol"
        override val use = listOf<RVRegister>() + arg.toList().subList(0, cnt)
        override val def = listOf<RVRegister>() + arg + temp + RVRegister.RA
        override fun transform(map: Map<out RVRegister, RVRegister>) = this
    }

    class SPGrow(val function: RVFunction) : RVInstruction() {
        override fun toString() = "\taddi\t${RVRegister.SP}, ${RVRegister.SP}, -${function.size}"
        override val use = listOf(RVRegister.SP)
        override val def = listOf(RVRegister.SP)
        override fun transform(map: Map<out RVRegister, RVRegister>) = this
    }

    class SPRecover(val function: RVFunction) : RVInstruction() {
        override fun toString() = "\taddi\t${RVRegister.SP}, ${RVRegister.SP}, ${function.size}"
        override val use = listOf(RVRegister.SP)
        override val def = listOf(RVRegister.SP)
        override fun transform(map: Map<out RVRegister, RVRegister>) = this
    }
}
