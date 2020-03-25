package personal.wuqing.rogue.llvm.grammar

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.llvm.IRTranslator

sealed class IRFunction(val ret: IRType, val args: List<IRType>, val name: String) {
    val display = "@$name"

    class Declared(
        ret: IRType, val namedArgs: List<IRItem>, name: String,
        val ast: ASTNode.Declaration.Function, val member: Boolean
    ) : IRFunction(ret, namedArgs.map { it.type }, name) {
        val body by lazy(LazyThreadSafetyMode.NONE) { IRTranslator.processBody(this) }
        val declaration = "define $ret $display(${namedArgs.joinToString { "${it.type} ${it.display}" }})"
    }

    sealed class External(ret: IRType, args: List<IRType>, name: String) : IRFunction(ret, args, name) {
        val declaration = "declare $ret $display(${args.joinToString()})"

        private companion object {
            val I8 = IRType.I8
            val I8P = IRType.Pointer(I8)
            val I32 = IRType.I32
            val Void = IRType.Void
            val str = IRType.string
        }

        object Malloc : External(I8P, listOf(I32), "malloc")
        object MallocArray : External(I8P, listOf(I32, I32), "_malloc_a_")
        object GetInt : External(I32, listOf(), "_get_i_")
        object GetString : External(str, listOf(), "_get_s_")
        object Print : External(Void, listOf(str), "_print_s_")
        object Println : External(Void, listOf(str), "_println_s_")
        object PrintInt : External(Void, listOf(I32), "_print_i_")
        object PrintlnInt : External(Void, listOf(I32), "_println_i_")
        object ToString : External(str, listOf(I32), "_to_str_")
        object StringLiteral : External(str, listOf(str, I32), "_s_literal_")
        object StringLength : External(I32, listOf(str), "_s_length_")
        object StringParse : External(I32, listOf(str), "_s_parse_")
        object StringOrd : External(I32, listOf(str, I32), "_s_ord_")
        object StringSubstring : External(str, listOf(str, I32, I32), "_s_substring_")
        object StringConcatenate : External(str, listOf(str, str), "_s_concatenate_")
        object StringEqual : External(I8, listOf(str, str), "_s_equal_")
        object StringNeq : External(I8, listOf(str, str), "_s_neq_")
        object StringLess : External(I8, listOf(str, str), "_s_less_")
        object StringLeq : External(I8, listOf(str, str), "_s_leq_")
        object StringGreater : External(I8, listOf(str, str), "_s_greater_")
        object StringGeq : External(I8, listOf(str, str), "_s_geq_")
        object ArraySize : External(I32, listOf(I8P), "_a_size_")
    }
}
