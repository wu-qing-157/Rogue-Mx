package personal.wuqing.mxcompiler.llvm.map

import personal.wuqing.mxcompiler.grammar.MxFunction
import personal.wuqing.mxcompiler.llvm.LLVMTranslator
import personal.wuqing.mxcompiler.llvm.grammar.LLVMFunction
import personal.wuqing.mxcompiler.llvm.grammar.LLVMName

object FunctionMap {
    private val map = mutableMapOf<MxFunction, LLVMFunction>()

    private fun MxFunction.llvmName() = when (this) {
        is MxFunction.Top -> if (name == "main") "main" else "__toplevel__.$name"
        is MxFunction.Member -> "$base.$name"
        MxFunction.Builtin.Print -> "__print__"
        MxFunction.Builtin.Println -> "__println__"
        MxFunction.Builtin.PrintInt -> "__printInt__"
        MxFunction.Builtin.PrintlnInt -> "__printlnInt__"
        MxFunction.Builtin.GetString -> "__getString__"
        MxFunction.Builtin.GetInt -> "__getInt__"
        MxFunction.Builtin.ToString -> "__toString__"
        MxFunction.Builtin.StringLength -> "__string__length__"
        MxFunction.Builtin.StringParseInt -> "__string__parseInt__"
        is MxFunction.Builtin.ArraySize -> "__array__size__"
        is MxFunction.Builtin.DefaultConstructor -> "__empty__"
        MxFunction.Builtin.StringOrd -> "__string__ord__"
        MxFunction.Builtin.StringSubstring -> "__string__substring__"
        is MxFunction.Builtin -> name // string binary operators
    }

    operator fun get(f: MxFunction) = map[f] ?: if (f is MxFunction.Builtin)
        (when (f) {
            MxFunction.Builtin.Print -> LLVMFunction.External.Print
            MxFunction.Builtin.Println -> LLVMFunction.External.Println
            MxFunction.Builtin.PrintInt -> LLVMFunction.External.PrintInt
            MxFunction.Builtin.PrintlnInt -> LLVMFunction.External.PrintlnInt
            MxFunction.Builtin.GetString -> LLVMFunction.External.GetString
            MxFunction.Builtin.GetInt -> LLVMFunction.External.GetInt
            MxFunction.Builtin.ToString -> LLVMFunction.External.ToString
            MxFunction.Builtin.StringLength -> LLVMFunction.External.StringLength
            MxFunction.Builtin.StringParseInt -> LLVMFunction.External.StringParseInt
            is MxFunction.Builtin.ArraySize -> LLVMFunction.External.ArraySize
            is MxFunction.Builtin.DefaultConstructor -> throw Exception("analyzing default constructor")
            MxFunction.Builtin.StringOrd -> LLVMFunction.External.StringOrd
            MxFunction.Builtin.StringSubstring -> LLVMFunction.External.StringSubstring
            MxFunction.Builtin.Malloc -> LLVMFunction.External.Malloc
            MxFunction.Builtin.MallocArray -> LLVMFunction.External.MallocArray
            MxFunction.Builtin.StringConcatenate -> LLVMFunction.External.StringConcatenate
            MxFunction.Builtin.StringEqual -> LLVMFunction.External.StringEqual
            MxFunction.Builtin.StringNeq -> LLVMFunction.External.StringNeq
            MxFunction.Builtin.StringLess -> LLVMFunction.External.StringLess
            MxFunction.Builtin.StringLeq -> LLVMFunction.External.StringLeq
            MxFunction.Builtin.StringGreater -> LLVMFunction.External.StringGreater
            MxFunction.Builtin.StringGeq -> LLVMFunction.External.StringGeq
        }).also { map[f] = it }
    else when (f) {
        is MxFunction.Top -> LLVMFunction.Declared(
            TypeMap[f.result], f.llvmName(), f.parameters.map { TypeMap[it] },
            f.def.parameterList.map { LLVMName.Local("__p__.${it.name}") }, false, f.def
        ).also { map[f] = it }.also { LLVMTranslator.toProcess += it }
        is MxFunction.Member -> LLVMFunction.Declared(
            TypeMap[f.def.returnType], f.llvmName(), (f.parameters + f.base).map { TypeMap[it] },
            f.def.parameterList.map { LLVMName.Local("__p__.${it.name}") } + LLVMName.Local("__this__"),
            true, f.def
        ).also { map[f] = it }.also { LLVMTranslator.toProcess += it }
        is MxFunction.Builtin -> throw Exception("declared map resolved as builtin")
    }

    fun all() = map.values
}
