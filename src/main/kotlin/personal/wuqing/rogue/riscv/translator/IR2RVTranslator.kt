package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRCmpOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.riscv.grammar.RVAddress
import personal.wuqing.rogue.riscv.grammar.RVBlock
import personal.wuqing.rogue.riscv.grammar.RVCalcOp
import personal.wuqing.rogue.riscv.grammar.RVCmpOp
import personal.wuqing.rogue.riscv.grammar.RVFunction
import personal.wuqing.rogue.riscv.grammar.RVGlobal
import personal.wuqing.rogue.riscv.grammar.RVInstruction
import personal.wuqing.rogue.riscv.grammar.RVLiteral
import personal.wuqing.rogue.riscv.grammar.RVProgram
import personal.wuqing.rogue.riscv.grammar.RVRegister
import personal.wuqing.rogue.riscv.grammar.RVRegister.Companion.arg
import personal.wuqing.rogue.riscv.grammar.RVRegister.Companion.saved
import kotlin.math.min

object IR2RVTranslator {
    operator fun invoke(program: IRProgram) = RVProgram(
        global = program.global.map { this(it) },
        literal = program.literal.map { this(it) },
        function = program.function.map { this(it) }
    )

    private val globalMap = mutableMapOf<IRItem.Global, RVGlobal>()
    private val literalMap = mutableMapOf<IRItem.Literal, RVLiteral>()
    private val localMap = mutableMapOf<IRItem.Local, RVRegister>()
    private val phiMap = mutableMapOf<IRStatement.Phi, RVRegister>()
    private val blockMap = mutableMapOf<IRBlock, RVBlock>()

    private fun <K> MutableMap<K, RVRegister>.virtual(key: K) = computeIfAbsent(key) { RVRegister.Virtual() }

    private val boolDef = mutableMapOf<IRItem.Local, IRStatement.Normal.ICmp>()
    private val addiDef = mutableMapOf<IRItem.Local, IRStatement.Normal.ICalc>()

    private operator fun invoke(global: IRItem.Global) = globalMap.computeIfAbsent(global) { RVGlobal(it.name) }

    private operator fun invoke(literal: IRItem.Literal) =
        literalMap.computeIfAbsent(literal) { RVLiteral(it.name, it.length, it.asmDisplay) }

    private fun scanUsage(function: IRFunction.Declared) {
        for (block in function.body) block.normal.forEach {
            when (it) {
                is IRStatement.Normal.ICmp -> boolDef[it.result] = it
                is IRStatement.Normal.ICalc ->
                    if (it.operator == IRCalcOp.ADD && it.op1 is IRItem.Local && it.op2 is IRItem.Const &&
                        it.op2.value in -2048..2047
                    ) addiDef[it.result] = it
                else -> Unit
            }
        }
        for (block in function.body) {
            for (phi in block.phi) phi.use.forEach {
                if (it in boolDef) boolDef.remove(it)
                if (it in addiDef) addiDef.remove(it)
            }
            for (normal in block.normal) normal.use.forEach {
                if (it in boolDef) boolDef.remove(it)
                if (normal !is IRStatement.Normal.Load
                    && (normal !is IRStatement.Normal.Store || it != normal.dest)
                    && it in addiDef
                ) addiDef.remove(it)
            }
            block.terminate.use.forEach {
                if (block.terminate !is IRStatement.Terminate.Branch && it in boolDef) boolDef.remove(it)
                if (it in addiDef) addiDef.remove(it)
            }
        }
    }

    private fun operator(op: IRCalcOp) = when (op) {
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
    }

    private fun operator(op: IRCmpOp) = when (op) {
        IRCmpOp.EQ -> RVCmpOp.EQ
        IRCmpOp.NE -> RVCmpOp.NE
        IRCmpOp.SLT -> RVCmpOp.LT
        IRCmpOp.SLE -> RVCmpOp.LE
        IRCmpOp.SGT -> RVCmpOp.GT
        IRCmpOp.SGE -> RVCmpOp.GE
    }

    private operator fun invoke(function: IRFunction.Declared) = RVFunction(function.name).apply {
        scanUsage(function)
        val ra = RVRegister.Virtual()
        val savedMap = saved.toList().associateWith { RVRegister.Virtual() }
        body += RVBlock("$function").also { ret ->
            ret.instructions += RVInstruction.SPGrow(this)
            ret.instructions += RVInstruction.Move(ra, RVRegister.RA)
            ret.instructions += savedMap.map { (s, v) -> RVInstruction.Move(v, s) }
            function.args.withIndex().forEach { (index, value) ->
                if (index < 8) localMap[value] = RVRegister.Virtual().also {
                    ret.instructions += RVInstruction.Move(it, arg[index])
                } else localMap[value] = RVRegister.Spilled(RVAddress.Caller(this, index))
            }
        }
        blockMap += function.body.associateWith { RVBlock("${function.name}...${it.name}") }
        body += function.body.map { block ->
            val ret = blockMap[block] ?: error("cannot find block")

            fun asRegister(item: IRItem) = when (item) {
                is IRItem.Local -> localMap.virtual(item)
                is IRItem.Global -> RVRegister.Virtual().also { virtual ->
                    ret.instructions +=
                        RVInstruction.LG(virtual, globalMap[item] ?: error("no global def"))
                }
                is IRItem.Const -> if (item.value == 0) RVRegister.ZERO else RVRegister.Virtual().also { virtual ->
                    ret.instructions += RVInstruction.LI(virtual, item.value)
                }
                is IRItem.Literal -> RVRegister.Virtual().also { virtual ->
                    ret.instructions += RVInstruction.LA(virtual, literalMap[item] ?: error("no literal def"))
                }
            }

            block.phi.forEach {
                ret.instructions += RVInstruction.Move(localMap.virtual(it.result), phiMap.virtual(it))
            }
            block.normal.forEach {
                when (it) {
                    is IRStatement.Normal.Load -> ret.instructions += when (it.src) {
                        is IRItem.Local -> addiDef[it.src]?.let { addi ->
                            RVInstruction.Load(
                                localMap.virtual(it.dest),
                                RVAddress(localMap.virtual(addi.op1 as IRItem.Local), (addi.op2 as IRItem.Const).value)
                            )
                        } ?: RVInstruction.Load(localMap.virtual(it.dest), RVAddress(localMap.virtual(it.src)))
                        is IRItem.Global ->
                            RVInstruction.LG(localMap.virtual(it.dest), globalMap[it.src] ?: error("no global def"))
                        is IRItem.Const ->
                            RVInstruction.Move(RVRegister.ZERO, RVRegister.ZERO)
                        else -> error("something cannot be loaded found")
                    }
                    is IRStatement.Normal.Store -> ret.instructions += when (it.dest) {
                        is IRItem.Local -> addiDef[it.dest]
                            ?.takeIf { addi -> (addi.op2 as IRItem.Const).value in -2048..2047 }
                            ?.let { addi ->
                                RVInstruction.Save(
                                    asRegister(it.src), RVAddress(
                                        localMap.virtual(addi.op1 as IRItem.Local), (addi.op2 as IRItem.Const).value
                                    )
                                )
                            } ?: RVInstruction.Save(asRegister(it.src), RVAddress(localMap.virtual(it.dest)))
                        is IRItem.Global -> RVInstruction.SG(
                            asRegister(it.src), RVRegister.Virtual(), globalMap[it.dest] ?: error("no global def")
                        )
                        else -> error("something cannot be saved into found")
                    }
                    is IRStatement.Normal.Alloca -> error("codegen without SSA")
                    is IRStatement.Normal.ICalc -> if (it.result !in addiDef) ret.instructions +=
                        if (operator(it.operator).imm != null && it.op2 is IRItem.Const && it.op2.value in -2048..2047)
                            if (it.operator == IRCalcOp.ADD && it.op2.value == 0)
                                RVInstruction.Move(localMap.virtual(it.result), asRegister(it.op1))
                            else RVInstruction.CalcI(
                                operator(it.operator), asRegister(it.op1), it.op2.value, localMap.virtual(it.result)
                            )
                        else RVInstruction.Calc(
                            operator(it.operator), asRegister(it.op1), asRegister(it.op2), localMap.virtual(it.result)
                        )
                    is IRStatement.Normal.ICmp -> if (it.result !in boolDef) {
                        val op1 = asRegister(it.op1)
                        val op2 = asRegister(it.op2)
                        val result = localMap.virtual(it.result)
                        when (it.operator) {
                            IRCmpOp.EQ -> {
                                val virtual = RVRegister.Virtual()
                                ret.instructions += RVInstruction.Calc(RVCalcOp.MINUS, op1, op2, virtual)
                                ret.instructions += RVInstruction.CmpZ(RVCmpOp.EQ, virtual, result)
                            }
                            IRCmpOp.NE -> {
                                val virtual = RVRegister.Virtual()
                                ret.instructions += RVInstruction.Calc(RVCalcOp.MINUS, op1, op2, virtual)
                                ret.instructions += RVInstruction.CmpZ(RVCmpOp.NE, virtual, result)
                            }
                            IRCmpOp.SLT -> ret.instructions += RVInstruction.Calc(RVCalcOp.LT, op1, op2, result)
                            IRCmpOp.SLE -> {
                                val virtual = RVRegister.Virtual()
                                ret.instructions += RVInstruction.Calc(RVCalcOp.LT, op2, op1, virtual)
                                ret.instructions += RVInstruction.CalcI(RVCalcOp.XOR, virtual, 1, result)
                            }
                            IRCmpOp.SGT -> ret.instructions += RVInstruction.Calc(RVCalcOp.LT, op2, op1, result)
                            IRCmpOp.SGE -> {
                                val virtual = RVRegister.Virtual()
                                ret.instructions += RVInstruction.Calc(RVCalcOp.LT, op1, op2, virtual)
                                ret.instructions += RVInstruction.CalcI(RVCalcOp.XOR, virtual, 1, result)
                            }
                        }
                    }
                    is IRStatement.Normal.Call -> {
                        it.args.withIndex().forEach { (index, value) ->
                            if (index < 8) ret.instructions += RVInstruction.Move(arg[index], asRegister(value))
                            else ret.instructions += RVInstruction.Save(asRegister(value), getPass(index))
                        }
                        ret.instructions += RVInstruction.Call(it.function.name, min(it.args.size, 8))
                        it.result?.let { result ->
                            ret.instructions += RVInstruction.Move(localMap.virtual(result), arg[0])
                        }
                    }
                    IRStatement.Normal.NOP -> Unit
                }
            }
            when (val it = block.terminate) {
                is IRStatement.Terminate.Ret -> {
                    it.item?.let { ret.instructions += RVInstruction.Move(arg[0], asRegister(it)) }
                    ret.instructions += savedMap.map { (s, v) -> RVInstruction.Move(s, v) }
                    ret.instructions += RVInstruction.Move(RVRegister.RA, ra)
                    ret.instructions += RVInstruction.SPRecover(this)
                    ret.instructions += RVInstruction.Ret()
                }
                is IRStatement.Terminate.Branch -> {
                    val then = blockMap[it.then] ?: error("cannot find block")
                    val els = blockMap[it.els] ?: error("cannot find block")
                    for (phi in it.then.phi) ret.instructions += RVInstruction.Move(
                        phiMap.virtual(phi), asRegister(phi.list[block] ?: error("no current block in phi"))
                    )
                    ret.instructions += boolDef[it.cond]?.let { bool ->
                        RVInstruction.Branch(operator(bool.operator), asRegister(bool.op1), asRegister(bool.op2), then)
                    } ?: RVInstruction.Branch(RVCmpOp.NE, asRegister(it.cond), RVRegister.ZERO, then)
                    for (phi in it.els.phi) ret.instructions += RVInstruction.Move(
                        phiMap.virtual(phi), asRegister(phi.list[block] ?: error("no current block in phi"))
                    )
                    ret.instructions += RVInstruction.J(els)
                    ret.next += listOf(then, els)
                    then.prev += ret
                    els.prev += ret
                }
                is IRStatement.Terminate.Jump -> {
                    for (phi in it.dest.phi) ret.instructions += RVInstruction.Move(
                        phiMap.virtual(phi), asRegister(phi.list[block] ?:
                        error("no current block in phi"))
                    )
                    val dest = blockMap[it.dest] ?: error("cannot find block")
                    ret.instructions += RVInstruction.J(dest)
                    ret.next += dest
                    dest.prev += ret
                }
            }
            ret
        }
        body[0].next += body[1]
        body[1].prev += body[0]
    }
}
