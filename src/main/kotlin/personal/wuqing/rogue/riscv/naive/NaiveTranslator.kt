package personal.wuqing.rogue.riscv.naive

import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRCmpOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.riscv.RVAddress
import personal.wuqing.rogue.riscv.RVCalcOp
import personal.wuqing.rogue.riscv.RVCmpOp
import personal.wuqing.rogue.riscv.RVFunction
import personal.wuqing.rogue.riscv.RVGlobal
import personal.wuqing.rogue.riscv.RVInstruction
import personal.wuqing.rogue.riscv.RVLiteral
import personal.wuqing.rogue.riscv.RVProgram
import personal.wuqing.rogue.riscv.RVRegister
import personal.wuqing.rogue.riscv.RVRegister.Companion.arg
import personal.wuqing.rogue.riscv.RVRegister.Companion.temp

object NaiveTranslator {
    operator fun invoke(program: IRProgram) = RVProgram(
        global = program.global.map { this(it) },
        literal = program.literal.map { this(it) },
        function = program.function.map { this(it) }
    )

    private val globalMap = mutableMapOf<IRItem.Global, RVGlobal>()
    private val literalMap = mutableMapOf<IRItem.Literal, RVLiteral>()
    private val localMap = mutableMapOf<IRItem.Local, RVAddress>()
    private val phiMap = mutableMapOf<IRStatement.Phi, RVAddress>()

    private fun load(item: IRItem, reg: RVRegister) = when (item) {
        is IRItem.Local -> RVInstruction.Load(reg, localMap[item]!!)
        is IRItem.Global -> error("load global directory")
        is IRItem.Const -> RVInstruction.LI(reg, item.value)
        is IRItem.Literal -> RVInstruction.LA(reg, literalMap[item]!!)
    }

    private operator fun invoke(global: IRItem.Global) = globalMap.computeIfAbsent(global) { RVGlobal("..${it.name}") }

    private operator fun invoke(literal: IRItem.Literal) =
        literalMap.computeIfAbsent(literal) { RVLiteral("..${literal.name}", literal.length, literal.asmDisplay) }

    private operator fun invoke(function: IRFunction.Declared) = RVFunction(function.name).apply {
        nextSaver(RVRegister.RA)
        function.args.withIndex().forEach { (index, value) ->
            if (index < 8) body +=
                RVInstruction.Save(arg[index], localMap.computeIfAbsent(value) { nextStack() })
            else localMap[value] = RVAddress.Caller(this, index)

        }
        for (block in function.body) {
            body += RVInstruction.Label("${function.name}..${block.name}")
            for (phi in block.phi) {
                body += RVInstruction.Load(temp[0], phiMap.computeIfAbsent(phi) { nextStack() })
                body += RVInstruction.Save(temp[0], localMap.computeIfAbsent(phi.result) { nextStack() })
            }
            for (st in block.normal) when (st) {
                is IRStatement.Normal.Load -> {
                    when (st.src) {
                        is IRItem.Local -> {
                            body += RVInstruction.Load(temp[1], localMap.computeIfAbsent(st.src) { nextStack() })
                            body += RVInstruction.Load(temp[0], RVAddress(temp[1]))
                        }
                        is IRItem.Global -> body += RVInstruction.LG(temp[0], globalMap[st.src]!!)
                        else -> error("load invalid item")
                    }
                    body += RVInstruction.Save(temp[0], localMap.computeIfAbsent(st.dest) { nextStack() })
                }
                is IRStatement.Normal.Store -> {
                    body += load(st.src, temp[0])
                    when (st.dest) {
                        is IRItem.Local -> {
                            body += RVInstruction.Load(temp[1], localMap.computeIfAbsent(st.dest) { nextStack() })
                            body += RVInstruction.Save(temp[0], RVAddress(temp[1]))
                        }
                        is IRItem.Global -> body += RVInstruction.SG(temp[0], temp[1], globalMap[st.dest]!!)
                        else -> error("save into invalid item")
                    }
                }
                is IRStatement.Normal.Alloca -> error("alloca when codegen")
                is IRStatement.Normal.ICalc -> {
                    body += load(st.op1, temp[0])
                    body += load(st.op2, temp[1])
                    body += RVInstruction.Calc(
                        when (st.operator) {
                            IRCalcOp.ADD -> RVCalcOp.PLUS
                            IRCalcOp.SUB -> RVCalcOp.MINUS
                            IRCalcOp.MUL -> RVCalcOp.TIMES
                            IRCalcOp.SDIV -> RVCalcOp.DIV
                            IRCalcOp.SREM -> RVCalcOp.REM
                            IRCalcOp.AND -> RVCalcOp.AND
                            IRCalcOp.OR -> RVCalcOp.OR
                            IRCalcOp.XOR -> RVCalcOp.XOR
                            IRCalcOp.SHL -> RVCalcOp.SHL
                            IRCalcOp.ASHR -> RVCalcOp.SHR
                            IRCalcOp.LSHR -> RVCalcOp.USHR
                        }, temp[0], temp[1], temp[0]
                    )
                    body += RVInstruction.Save(temp[0], localMap.computeIfAbsent(st.result) { nextStack() })
                }
                is IRStatement.Normal.ICmp -> {
                    body += load(st.op1, temp[0])
                    body += load(st.op2, temp[1])
                    when (st.operator) {
                        IRCmpOp.EQ -> {
                            body += RVInstruction.Calc(RVCalcOp.MINUS, temp[0], temp[1], temp[0])
                            body += RVInstruction.CmpZ(RVCmpOp.EQ, temp[0], temp[0])
                        }
                        IRCmpOp.NE -> {
                            body += RVInstruction.Calc(RVCalcOp.MINUS, temp[0], temp[1], temp[0])
                            body += RVInstruction.CmpZ(RVCmpOp.NE, temp[0], temp[0])
                        }
                        IRCmpOp.SLT -> body += RVInstruction.Calc(RVCalcOp.LT, temp[0], temp[1], temp[0])
                        IRCmpOp.SLE -> {
                            body += RVInstruction.Calc(RVCalcOp.LT, temp[1], temp[0], temp[0])
                            body += RVInstruction.CalcI(RVCalcOp.XOR, temp[0], 1, temp[0])
                        }
                        IRCmpOp.SGT -> body += RVInstruction.Calc(RVCalcOp.LT, temp[1], temp[0], temp[0])
                        IRCmpOp.SGE -> {
                            body += RVInstruction.Calc(RVCalcOp.LT, temp[0], temp[1], temp[0])
                            body += RVInstruction.CalcI(RVCalcOp.XOR, temp[0], 1, temp[0])
                        }
                    }
                    body += RVInstruction.Save(temp[0], localMap.computeIfAbsent(st.result) { nextStack() })
                }
                is IRStatement.Normal.Call -> {
                    st.args.withIndex().forEach { (index, value) ->
                        if (index < 8) body += load(value, arg[index])
                        else {
                            body += load(value, temp[0])
                            body += RVInstruction.Save(temp[0], getPass(index))
                        }
                    }
                    body += RVInstruction.Call(st.function.name)
                    st.result?.let {
                        body += RVInstruction.Save(arg[0], localMap.computeIfAbsent(it) { nextStack() })
                    }
                }
                IRStatement.Normal.NOP -> Unit
            }
            for (next in block.next) for (phi in next.phi) {
                body += load(phi.list[block] ?: error("cannot find current block"), temp[0])
                body += RVInstruction.Save(temp[0], phiMap.computeIfAbsent(phi) { nextStack() })
            }
            when (val st = block.terminate) {
                is IRStatement.Terminate.Ret -> {
                    st.item?.let { body += load(it, arg[0]) }
                    body += RVInstruction.J("${function.name}..ret")
                }
                is IRStatement.Terminate.Branch -> {
                    body += load(st.cond, temp[0])
                    body +=
                        RVInstruction.Branch(RVCmpOp.NE, temp[0], RVRegister.ZERO, "${function.name}..${st.then.name}")
                    body += RVInstruction.J("${function.name}..${st.els.name}")
                }
                is IRStatement.Terminate.Jump -> body +=
                    RVInstruction.J("${function.name}..${st.dest.name}")
            }
        }
    }
}
