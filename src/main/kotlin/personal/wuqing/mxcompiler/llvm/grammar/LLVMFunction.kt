package personal.wuqing.mxcompiler.llvm.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.llvm.LLVMTranslator

sealed class LLVMFunction(
    val ret: LLVMType, val name: LLVMName, val args: List<LLVMType>
) {
    class Declared(
        ret: LLVMType, name: String, args: List<LLVMType>, val argName: List<LLVMName>, val member: Boolean,
        val ast: ASTNode.Declaration.Function
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        val body by lazy(LazyThreadSafetyMode.NONE) { LLVMTranslator.processBody(this) }
        fun definition() = "define $ret $name(${(args zip argName).joinToString { (t, n) -> "$t $n" }}) {"
    }

    sealed class External(
        ret: LLVMType, name: String, args: List<LLVMType>
    ) : LLVMFunction(ret, LLVMName.Global(name), args) {
        override fun toString() = "declare $ret $name(${args.joinToString()})"

        object Malloc : External(LLVMType.string, "malloc", listOf(LLVMType.I32))
        object MallocArray : External(LLVMType.string, "__malloc__array__", listOf(LLVMType.I32, LLVMType.I32))
        object GetInt : External(LLVMType.I32, "__getInt__", listOf())
        object GetString : External(LLVMType.string, "__getString__", listOf())
        object Print : External(LLVMType.Void, "__print__", listOf(LLVMType.string))
        object Println : External(LLVMType.Void, "__println__", listOf(LLVMType.string))
        object PrintInt : External(LLVMType.Void, "__printInt__", listOf(LLVMType.I32))
        object PrintlnInt : External(LLVMType.Void, "__printlnInt__", listOf(LLVMType.I32))
        object ToString : External(LLVMType.string, "__toString__", listOf(LLVMType.I32))
        object StringLength : External(LLVMType.I32, "__string__length__", listOf(LLVMType.string))
        object StringParseInt : External(LLVMType.I32, "__string__parseInt__", listOf(LLVMType.string))
        object StringOrd : External(LLVMType.I32, "__string__ord__", listOf(LLVMType.I32, LLVMType.string))

        object StringSubstring :
            External(LLVMType.string, "__string__substring__", listOf(LLVMType.I32, LLVMType.I32, LLVMType.string))

        object StringConcatenate :
            External(LLVMType.string, "__string__concatenate__", listOf(LLVMType.string, LLVMType.string))

        object StringEqual : External(LLVMType.I8, "__string__equal__", listOf(LLVMType.string, LLVMType.string))
        object StringNeq : External(LLVMType.I8, "__string__neq__", listOf(LLVMType.string, LLVMType.string))
        object StringLess : External(LLVMType.I8, "__string__less__", listOf(LLVMType.string, LLVMType.string))
        object StringLeq : External(LLVMType.I8, "__string__leq__", listOf(LLVMType.string, LLVMType.string))
        object StringGreater : External(LLVMType.I8, "__string__greater__", listOf(LLVMType.string, LLVMType.string))
        object StringGeq : External(LLVMType.I8, "__string__geq__", listOf(LLVMType.string, LLVMType.string))
        object ArraySize : External(LLVMType.I32, "__array__size__", listOf(LLVMType.string))
    }
}
