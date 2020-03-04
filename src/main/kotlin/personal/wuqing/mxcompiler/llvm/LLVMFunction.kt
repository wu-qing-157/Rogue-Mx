package personal.wuqing.mxcompiler.llvm

import personal.wuqing.mxcompiler.ast.ASTNode

sealed class LLVMFunction(
    val ret: LLVMType, val name: LLVMName, val args: List<LLVMType>
) {
    class Declared(
        ret: LLVMType, name: String, args: List<LLVMType>, private val ast: ASTNode.Declaration.Function
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        val body by lazy(LazyThreadSafetyMode.NONE) { Translator(ast) }
        fun definition() = "define $ret $name(${args.indices.joinToString {
            "${args[it]} ${LLVMName.Local("__p__.${ast.parameterList[it].name}")}"
        }}) {"
    }

    sealed class External(
        ret: LLVMType, name: String, args: List<LLVMType>
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        override fun toString() = "declare $ret $name(${args.joinToString()})"

        object Empty : External(LLVMType.Void, "__empty__", listOf())
        object Malloc : External(LLVMType.Pointer(LLVMType.Void), "malloc", listOf(LLVMType.I32))
        object GetInt : External(LLVMType.I32, "__getInt__", listOf())
        object GetString : External(LLVMType.string, "__getString__", listOf())
        object Print : External(LLVMType.Void, "__print__", listOf(LLVMType.string))
        object Println : External(LLVMType.Void, "__println__", listOf(LLVMType.string))
        object PrintInt : External(LLVMType.Void, "__printInt__", listOf(LLVMType.I32))
        object PrintlnInt : External(LLVMType.Void, "__printlnInt__", listOf(LLVMType.I32))
        object ToString : External(LLVMType.string, "__toString__", listOf(LLVMType.I32))
        object StringLength : External(LLVMType.I32, "__string__length__", listOf(LLVMType.string))
        object StringParseInt : External(LLVMType.I32, "__string__parseInt__", listOf(LLVMType.string))
        object StringOrd : External(LLVMType.I32, "__string__ord__", listOf(LLVMType.string, LLVMType.I32))

        object StringSubstring :
            External(LLVMType.string, "__string__substring__", listOf(LLVMType.string, LLVMType.I32, LLVMType.I32))

        object StringConcatenate :
            External(LLVMType.string, "__string__concatenate__", listOf(LLVMType.string, LLVMType.string))

        object StringEqual :
            External(LLVMType.I8, "__string__equal__", listOf(LLVMType.string, LLVMType.string))

        object StringNeq :
            External(LLVMType.I8, "__string__neq__", listOf(LLVMType.string, LLVMType.string))

        object StringLess :
            External(LLVMType.I8, "__string__less__", listOf(LLVMType.string, LLVMType.string))

        object StringLeq :
            External(LLVMType.I8, "__string__leq__", listOf(LLVMType.string, LLVMType.string))

        object StringGreater :
            External(LLVMType.I8, "__string__greater__", listOf(LLVMType.string, LLVMType.string))

        object StringGeq :
            External(LLVMType.I8, "__string__geq__", listOf(LLVMType.string, LLVMType.string))

        object ArraySize : External(LLVMType.I32, "__array__size__", listOf(LLVMType.Pointer(LLVMType.Void)))
    }
}
