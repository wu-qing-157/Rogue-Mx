package personal.wuqing.rogue.riscv

sealed class RVInstruction {
    class LI(val reg: RVRegister, val imm: Int) : RVInstruction() {
        override fun toString() = "\tli\t$reg, $imm"
    }

    class LA(val reg: RVRegister, val literal: RVLiteral) : RVInstruction() {
        override fun toString() = "\tla\t$reg, $literal"
    }

    class Load(val reg: RVRegister, val addr: RVAddress) : RVInstruction() {
        override fun toString() = "\tlw\t$reg, $addr"
    }

    class LG(val reg: RVRegister, val global: RVGlobal) : RVInstruction() {
        override fun toString() = "\tlw\t$reg, $global"
    }

    class Save(val reg: RVRegister, val addr: RVAddress) : RVInstruction() {
        override fun toString() = "\tsw\t$reg, $addr"
    }

    class SG(val reg: RVRegister, val assist: RVRegister, val global: RVGlobal) : RVInstruction() {
        override fun toString() = "\tsw\t$reg, $global, $assist"
    }

    class Move(val dest: RVRegister, val src: RVRegister) : RVInstruction() {
        override fun toString() = "\tmv\t$dest, $src"
    }

    class Calc(val op: RVCalcOp, val lhs: RVRegister, val rhs: RVRegister, val result: RVRegister) : RVInstruction() {
        override fun toString() = "\t${op.binary}\t$result, $lhs, $rhs"
    }

    class CalcI(val op: RVCalcOp, val lhs: RVRegister, val imm: Int, val result: RVRegister) : RVInstruction() {
        init {
            op.imm ?: error("invalid operator with immediate")
        }

        override fun toString() = "\t${op.imm}\t$result, $lhs, $imm"
    }

    class CmpZ(val op: RVCmpOp, val reg: RVRegister, val result: RVRegister) : RVInstruction() {
        init {
            op.zero ?: error("invalid operator comparing with zero")
        }

        override fun toString() = "\t${op.zero}\t$result, $reg"
    }

    class J(val symbol: String) : RVInstruction() {
        override fun toString() = "\tj\t$symbol"
    }

    class JR(val reg: RVRegister) : RVInstruction() {
        override fun toString() = "\tjr\t$reg"
    }

    class Branch(val op: RVCmpOp, val lhs: RVRegister, val rhs: RVRegister, val symbol: String) : RVInstruction() {
        override fun toString() = "\t${op.branch}\t$lhs, $rhs, $symbol"
    }

    class Call(val symbol: String) : RVInstruction() {
        override fun toString() = "\tcall\t$symbol"
    }

    class SPGrow(val function: RVFunction) : RVInstruction() {
        override fun toString() = "\taddi\t${RVRegister.SP}, ${RVRegister.SP}, -${function.size}"
    }

    class SPRecover(val function: RVFunction) : RVInstruction() {
        override fun toString() = "\taddi\t${RVRegister.SP}, ${RVRegister.SP}, ${function.size}"
    }
}
