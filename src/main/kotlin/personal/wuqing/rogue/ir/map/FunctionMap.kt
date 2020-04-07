package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRType
import personal.wuqing.rogue.ir.translator.TopLevelTranslator

object FunctionMap {
    private val map = mutableMapOf<MxFunction, IRFunction.Declared>()

    operator fun get(f: MxFunction) = map[f] ?: when (f) {
        is MxFunction.Top -> IRFunction.Declared(
            ret = TypeMap[f.result],
            namedArgs = f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type]) },
            name = f.name,
            ast = f.def,
            member = false
        ).also { map[f] = it }.also { TopLevelTranslator.toProcess += it }
        is MxFunction.Member -> IRFunction.Declared(
            ret = if (f.name == "__constructor__") IRType.Void else TypeMap[f.result],
            namedArgs = listOf(IRItem.Local(TypeMap[f.base]))
                    + f.def.parameterList.map { IRItem.Local(TypeMap[it.type.type]) },
            name = "${f.base}.${f.name}",
            ast = f.def,
            member = true
        ).also { map[f] = it }.also { TopLevelTranslator.toProcess += it }
        MxFunction.Builtin.Print -> IRFunction.External.Print
        MxFunction.Builtin.Println -> IRFunction.External.Println
        MxFunction.Builtin.PrintInt -> IRFunction.External.PrintInt
        MxFunction.Builtin.PrintlnInt -> IRFunction.External.PrintlnInt
        MxFunction.Builtin.GetString -> IRFunction.External.GetString
        MxFunction.Builtin.GetInt -> IRFunction.External.GetInt
        MxFunction.Builtin.ToString -> IRFunction.External.ToString
        MxFunction.Builtin.StringLength -> error("access string length using function call in IR")
        MxFunction.Builtin.StringParseInt -> IRFunction.External.StringParse
        is MxFunction.Builtin.ArraySize -> error("access array size using function call in IR")
        is MxFunction.Builtin.DefaultConstructor -> error("access default constructor in IR")
        MxFunction.Builtin.StringOrd -> IRFunction.External.StringOrd
        MxFunction.Builtin.StringSubstring -> IRFunction.External.StringSubstring
    }

    fun all() = map.values
}
