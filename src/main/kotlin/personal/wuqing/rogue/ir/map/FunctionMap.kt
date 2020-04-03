package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.ir.IRTranslator
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRType

object FunctionMap {
    private val map = mutableMapOf<MxFunction, IRFunction.Declared>()

    operator fun get(f: MxFunction) = map[f] ?: when (f) {
        is MxFunction.Top -> IRFunction.Declared(
            ret = TypeMap[f.result],
            namedArgs = f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type]) },
            name = f.name,
            ast = f.def,
            member = false
        ).also { map[f] = it }.also { IRTranslator.toProcess += it }
        is MxFunction.Member -> IRFunction.Declared(
            ret = if (f.name == "__constructor__") IRType.Void else TypeMap[f.result],
            namedArgs = listOf(IRItem.Local(TypeMap[f.base]))
                    + f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type]) },
            name = "${f.base}.${f.name}",
            ast = f.def,
            member = true
        ).also { map[f] = it }.also { IRTranslator.toProcess += it }
        MxFunction.Builtin.Print -> TODO()
        MxFunction.Builtin.Println -> TODO()
        MxFunction.Builtin.PrintInt -> TODO()
        MxFunction.Builtin.PrintlnInt -> TODO()
        MxFunction.Builtin.GetString -> TODO()
        MxFunction.Builtin.GetInt -> TODO()
        MxFunction.Builtin.ToString -> TODO()
        MxFunction.Builtin.StringLength -> TODO()
        MxFunction.Builtin.StringParseInt -> TODO()
        is MxFunction.Builtin.ArraySize -> TODO()
        is MxFunction.Builtin.DefaultConstructor -> TODO()
        MxFunction.Builtin.StringLiteral -> TODO()
        MxFunction.Builtin.StringOrd -> TODO()
        MxFunction.Builtin.StringSubstring -> TODO()
        MxFunction.Builtin.Malloc -> TODO()
        MxFunction.Builtin.MallocArray -> TODO()
        MxFunction.Builtin.StringConcatenate -> TODO()
        MxFunction.Builtin.StringEqual -> TODO()
        MxFunction.Builtin.StringNeq -> TODO()
        MxFunction.Builtin.StringLess -> TODO()
        MxFunction.Builtin.StringLeq -> TODO()
        MxFunction.Builtin.StringGreater -> TODO()
        MxFunction.Builtin.StringGeq -> TODO()
    }

    fun all() = map.values
}
