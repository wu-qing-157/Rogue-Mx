package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.translator.TopLevelTranslator

object FunctionMap {
    private val map = mutableMapOf<MxFunction, IRFunction.Declared>()

    operator fun get(f: MxFunction) = map[f] ?: when (f) {
        is MxFunction.Top -> IRFunction.Declared(
            args = f.def.parameterList.map { IRItem.Local() },
            name = f.name,
            ast = f.def,
            member = false
        ).also { map[f] = it }.also { TopLevelTranslator.toProcess += it }
        is MxFunction.Member -> IRFunction.Declared(
            args = Array(f.def.parameterList.size + 1) { IRItem.Local() }.toList(),
            name = "${f.base}.${f.name}",
            ast = f.def,
            member = true
        ).also { map[f] = it }.also { TopLevelTranslator.toProcess += it }
        MxFunction.Builtin.Print -> IRFunction.Builtin.Print
        MxFunction.Builtin.Println -> IRFunction.Builtin.Println
        MxFunction.Builtin.PrintInt -> IRFunction.Builtin.PrintInt
        MxFunction.Builtin.PrintlnInt -> IRFunction.Builtin.PrintlnInt
        MxFunction.Builtin.GetString -> IRFunction.Builtin.GetString
        MxFunction.Builtin.GetInt -> IRFunction.Builtin.GetInt
        MxFunction.Builtin.ToString -> IRFunction.Builtin.ToString
        MxFunction.Builtin.StringLength -> error("access string length using function call in IR")
        MxFunction.Builtin.StringParseInt -> IRFunction.Builtin.StringParse
        is MxFunction.Builtin.ArraySize -> error("access array size using function call in IR")
        is MxFunction.Builtin.DefaultConstructor -> error("access default constructor in IR")
        MxFunction.Builtin.StringOrd -> IRFunction.Builtin.StringOrd
        MxFunction.Builtin.StringSubstring -> IRFunction.Builtin.StringSubstring
    }

    fun all() = map.values
}
