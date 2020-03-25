package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.llvm.IRTranslator
import personal.wuqing.rogue.llvm.grammar.IRFunction
import personal.wuqing.rogue.llvm.grammar.IRItem
import personal.wuqing.rogue.llvm.grammar.IRType

object FunctionMap {
    private val map = mutableMapOf<MxFunction, IRFunction>()

    private fun MxFunction.Top.llvmName() = if (name == "main") "main" else "_top_.$name"

    operator fun get(f: MxFunction) = map[f] ?: if (f is MxFunction.Builtin)
        when (f) {
            MxFunction.Builtin.Print -> IRFunction.External.Print
            MxFunction.Builtin.Println -> IRFunction.External.Println
            MxFunction.Builtin.PrintInt -> IRFunction.External.PrintInt
            MxFunction.Builtin.PrintlnInt -> IRFunction.External.PrintlnInt
            MxFunction.Builtin.GetString -> IRFunction.External.GetString
            MxFunction.Builtin.GetInt -> IRFunction.External.GetInt
            MxFunction.Builtin.ToString -> IRFunction.External.ToString
            MxFunction.Builtin.StringLength -> IRFunction.External.StringLength
            MxFunction.Builtin.StringParseInt -> IRFunction.External.StringParse
            is MxFunction.Builtin.ArraySize -> IRFunction.External.ArraySize
            is MxFunction.Builtin.DefaultConstructor -> throw Exception("analyzing default constructor")
            MxFunction.Builtin.StringLiteral -> IRFunction.External.StringLiteral
            MxFunction.Builtin.StringOrd -> IRFunction.External.StringOrd
            MxFunction.Builtin.StringSubstring -> IRFunction.External.StringSubstring
            MxFunction.Builtin.Malloc -> IRFunction.External.Malloc
            MxFunction.Builtin.MallocArray -> IRFunction.External.MallocArray
            MxFunction.Builtin.StringConcatenate -> IRFunction.External.StringConcatenate
            MxFunction.Builtin.StringEqual -> IRFunction.External.StringEqual
            MxFunction.Builtin.StringNeq -> IRFunction.External.StringNeq
            MxFunction.Builtin.StringLess -> IRFunction.External.StringLess
            MxFunction.Builtin.StringLeq -> IRFunction.External.StringLeq
            MxFunction.Builtin.StringGreater -> IRFunction.External.StringGreater
            MxFunction.Builtin.StringGeq -> IRFunction.External.StringGeq
        }.also { map[f] = it }
    else when (f) {
        is MxFunction.Top -> IRFunction.Declared(
            ret = TypeMap[f.result],
            namedArgs = f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type], "p.${it.name}") },
            name = f.llvmName(),
            ast = f.def,
            member = false
        ).also { map[f] = it }.also { IRTranslator.toProcess += it }
        is MxFunction.Member -> IRFunction.Declared(
            ret = if (f.name == "__constructor__") IRType.Void else TypeMap[f.result],
            namedArgs = listOf(IRItem.Local(TypeMap[f.base], "p.this"))
                    + f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type], "p.${it.name}") },
            name = "${f.base}.${f.name}",
            ast = f.def,
            member = true
        ).also { map[f] = it }.also { IRTranslator.toProcess += it }
        is MxFunction.Builtin -> throw Exception("declared map resolved as builtin")
    }

    fun all() = map.values
}
