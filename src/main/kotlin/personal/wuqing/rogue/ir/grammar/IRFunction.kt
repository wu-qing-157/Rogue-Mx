package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ir.translator.TopLevelTranslator

sealed class IRFunction(val ret: IRType, val args: List<IRType>, val name: String) {
    class Declared(
        ret: IRType, val namedArgs: List<IRItem>, name: String,
        val ast: ASTNode.Declaration.Function, val member: Boolean
    ) : IRFunction(ret, namedArgs.map { it.type }, name) {
        val body by lazy(LazyThreadSafetyMode.NONE) { TopLevelTranslator(this) }
    }

    sealed class External(ret: IRType, args: List<IRType>, name: String) : IRFunction(ret, args, name) {
        private companion object {
            val i32 = IRType.I32
            val i1 = IRType.I1
            val void_ = IRType.Void
            val str = IRType.String
        }

        object GetInt : External(i32, listOf(), "_get_i_")
        object GetString : External(str, listOf(), "_get_s_")
        object Print : External(void_, listOf(str), "_print_s_")
        object Println : External(void_, listOf(str), "_println_s_")
        object PrintInt : External(void_, listOf(i32), "_print_i_")
        object PrintlnInt : External(void_, listOf(i32), "_println_i_")
        object ToString : External(str, listOf(i32), "_to_str_")
        object StringParse : External(i32, listOf(str), "_s_parse_")
        object StringOrd : External(i32, listOf(str, i32), "_s_ord_")
        object StringSubstring : External(str, listOf(str, i32, i32), "_s_substring_")
        object StringConcatenate : External(str, listOf(str, str), "_s_concatenate_")
        object StringEqual : External(i1, listOf(str, str), "_s_equal_")
        object StringNeq : External(i1, listOf(str, str), "_s_neq_")
        object StringLess : External(i1, listOf(str, str), "_s_less_")
        object StringLeq : External(i1, listOf(str, str), "_s_leq_")
        object StringGreater : External(i1, listOf(str, str), "_s_greater_")
        object StringGeq : External(i1, listOf(str, str), "_s_geq_")
    }
}
