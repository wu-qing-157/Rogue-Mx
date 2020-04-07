package personal.wuqing.rogue.ir

import personal.wuqing.rogue.A64
import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRFunction.External.*
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType

object LLVMPrinter {
    private var nameCount = 0
    private val name = mutableMapOf<IRItem, String>()
    private fun name(item: IRItem) = name.computeIfAbsent(item) {
        when (it) {
            is IRItem.Local -> "%v${nameCount++}"
            is IRItem.Global -> "@g${nameCount++}"
            is IRItem.Const -> "${it.value}"
            is IRItem.Null -> error("requiring name of null in LLVM")
            is IRItem.Literal -> "@li${nameCount++}"
        }
    }

    private var tempCount = 0
    private fun temp() = "%llvm${tempCount++}"

    operator fun invoke(program: IRProgram): String {
        val result = StringBuilder()
        program.global.joinTo(result) { global(it) }
        result.appendln()
        program.literal.forEach { literal(result, it) }
        result.appendln()
        program.function.forEach { func(result, it) }
        result.appendln()
        external.joinTo(result, "\n")
        result.appendln()
        return result.toString()
    }

    private fun global(item: IRItem.Global) = "${name(item)} = global ${item.type} 0 ; type: ${item.type}"

    private fun literal(result: StringBuilder, item: IRItem.Literal) = temp().let {
        result.appendln("$it = constant {i32, [${item.length + 1} x i8]} {i32 ${item.length}, [${item.length + 1} x i8] c\"${item.llvmFormat}\"}")
        result.appendln("${name(item)} = constant {i32, i8*}* bitcast({i32, [4 x i8]}* $it to {i32, i8*}*)")
    }

    private fun func(result: StringBuilder, func: IRFunction.Declared) {
        result.appendln("define ${func.ret} @${func.name}(${func.namedArgs.joinToString { "${it.type} ${name(it)}" }}) {")
        func.body.forEach { block(result, it) }
        result.appendln("}")
    }

    private fun block(result: StringBuilder, block: IRBlock) {
        result.appendln("${block.name}:")
        block.phi.forEach { this(result, it) }
        block.normal.forEach { this(result, it) }
        this(result, block.terminate)
    }

    private operator fun invoke(builder: StringBuilder, phi: IRStatement.Phi) = phi.run {
        builder.appendln("    ${name(result)} = phi ${result.type} ${list.forEach { (item, block) ->
            "%${block.name} ${name(item)}"
        }}")
    }

    private operator fun invoke(builder: StringBuilder, normal: IRStatement.Normal): Unit = normal.run {
        when (this) {
            is IRStatement.Normal.Load ->
                dest.type.let { type ->
                    builder.appendln("    ${name(dest)} = load $type, $type* ${name(src)}")
                }
            is IRStatement.Normal.Store ->
                src.type.let { type ->
                    builder.appendln("    store $type ${name(src)}, $type* ${name(dest)}")
                }
            is IRStatement.Normal.Alloca ->
                builder.appendln("    ${name(item)} = " +
                        "alloca ${(item.type as? IRType.Address ?: error("alloca non-address in LLVM")).type}")
            is IRStatement.Normal.MallocObject -> {
                val temp = temp()
                builder.appendln("    $temp = call i32* @_malloc_o_(i32 $size)")
                builder.appendln("    ${name(item)} = bitcast i32* $temp to ${item.type}")
            }
            is IRStatement.Normal.MallocArray -> {
                val temp = temp()
                builder.appendln("    $temp = call i32* @_malloc_a_(i32 $single, i32 ${name(length)})")
                builder.appendln("    ${name(item)} = bitcast i32* $temp to ${item.type}")
            }
            is IRStatement.Normal.ICalc ->
                builder.appendln("    ${name(result)} = $operator ${result.type} ${name(op1)}, ${name(op2)}")
            is IRStatement.Normal.ICmp ->
                builder.appendln("    ${name(result)} = $operator ${op1.type} ${name(op1)}, ${name(op2)}")
            is IRStatement.Normal.Call ->
                builder.appendln("    " +
                        (if (result.type is IRType.Void) "call void" else "${name(result)} = call ${result.type}") +
                        " @${function.name}(${args.joinToString { "${it.type} ${name(it)}" }})")
            is IRStatement.Normal.Member ->
                builder.appendln(
                    "${name(result)} = getelementptr ${base.type.toString().removeSuffix("*")}, " +
                            "${base.type} ${name(base)}, i32 0, i32 $index"
                )
            is IRStatement.Normal.Index ->
                builder.appendln("${name(result)} = getelementptr ${array.type.toString().removeSuffix("*")}, " +
                        "${array.type} ${name(array)}, i32 0, i32 1, i32 ${name(index)}")
            is IRStatement.Normal.Size -> {
                val temp = temp()
                builder.appendln("$temp = getelementptr ${when (base.type) {
                    is IRType.Array -> base.type.reference
                    is IRType.Class -> base.type.reference
                    else -> error("access size on neither class nor array")
                }}, ${base.type} ${name(base)}, i32 0, i32 0")
                builder.appendln("${name(result)} = load i32, i32* $temp")
            }
            IRStatement.Normal.Nop -> Unit
        }
    }

    private operator fun invoke(builder: StringBuilder, terminate: IRStatement.Terminate) = terminate.run {
        when (this) {
            is IRStatement.Terminate.Ret ->
                builder.appendln(item?.let { "    ret ${it.type} ${name(it)}" } ?: "ret void")
            is IRStatement.Terminate.Branch ->
                builder.appendln("    br i1 ${name(cond)}, label %${then.name}, label %${els.name}")
            is IRStatement.Terminate.Jump ->
                builder.appendln("    br label %${dest.name}")
        }
    }

    private val external
        get() =
            listOf(
                GetInt, GetString, Print, Println, PrintInt, PrintlnInt, ToString,
                StringParse, StringOrd, StringSubstring,
                StringConcatenate, StringEqual, StringNeq, StringLess, StringLeq, StringGreater, StringGeq
            ).map {
                "declare ${it.ret} @${it.name}(${it.args.joinToString()})"
            } + "declare $ptr @_malloc_o_(i32)" + "declare $ptr @_malloc_a_(i32)"

    private val ptr get() = if (A64) "i64" else "i32"
}
