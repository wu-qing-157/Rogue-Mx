package personal.wuqing.mxcompiler.llvm

import personal.wuqing.mxcompiler.ast.ASTNode

sealed class LLVMFunction(
    val ret: LLVMType, val name: LLVMName, val args: List<LLVMType>
) {
    class Declared(
        ret: LLVMType, name: String, args: List<LLVMType>, private val ast: ASTNode.Declaration.Function
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        val body by lazy(LazyThreadSafetyMode.NONE) { Translator(ast) }
        fun definition() = "define $ret $name(${args.joinToString()}) {"
    }

    sealed class External(
        ret: LLVMType, name: String, args: List<LLVMType>
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        object Empty : External(LLVMType.Void, "__empty__", listOf())
        object Malloc : External(LLVMType.Pointer(LLVMType.Void), "malloc", listOf(LLVMType.I(32)))
        object GetInt : External(LLVMType.I(32), "__getInt__", listOf())
        object GetString : External(LLVMType.String, "__getString__", listOf())
        object Print : External(LLVMType.Void, "__print__", listOf(LLVMType.String))
        object Println : External(LLVMType.Void, "__println__", listOf(LLVMType.String))
        object PrintInt : External(LLVMType.Void, "__printInt__", listOf(LLVMType.I(32)))
        object PrintlnInt : External(LLVMType.Void, "__printlnInt__", listOf(LLVMType.I(32)))
        object ToString : External(LLVMType.String, "__toString__", listOf(LLVMType.I(32)))
        object StringLength : External(LLVMType.I(32), "__string__length__", listOf(LLVMType.String))
        object StringOrd : External(LLVMType.I(32), "__string__ord__", listOf(LLVMType.String, LLVMType.I(32)))
        object StringParseInt : External(LLVMType.I(32), "__string__parseInt__", listOf(LLVMType.String))
        object ArraySize : External(LLVMType.I(32), "__array__size__", listOf(LLVMType.Pointer(LLVMType.Void)))
        object StringSubstring : External(
            LLVMType.String, "__string__substring__", listOf(LLVMType.String, LLVMType.I(32), LLVMType.I(32))
        )
    }
}
