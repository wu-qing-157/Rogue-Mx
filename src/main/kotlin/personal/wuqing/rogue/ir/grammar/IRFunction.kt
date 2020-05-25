package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ir.translator.TopLevelTranslator

sealed class IRFunction(val name: String) {
    override fun toString() = name

    class Declared(
        val args: List<IRItem.Local>, name: String, val ast: ASTNode.Declaration.Function, val member: Boolean
    ) : IRFunction(name) {
        val body by lazy(LazyThreadSafetyMode.NONE) { TopLevelTranslator(this) }

        fun updatePrev() {
            body.forEach { it.prev.clear() }
            val visited = mutableSetOf<IRBlock>()
            fun visit(block: IRBlock) {
                if (block in visited) return
                visited += block
                for (n in block.next) {
                    n.prev += block
                    visit(n)
                }
            }
            visit(body[0])
            body.removeAll { it !in visited }
        }
    }

    sealed class Builtin(name: String) : IRFunction(name) {
        object MallocObject : Builtin("malloc")
        object MallocArray : Builtin("_malloc_a_")
        object GetInt : Builtin("_get_i_")
        object GetString : Builtin("_get_s_")
        object Print : Builtin("_print_s_")
        object Println : Builtin("_println_s_")
        object PrintInt : Builtin("_print_i_")
        object PrintlnInt : Builtin("_println_i_")
        object ToString : Builtin("_to_str_")
        object StringParse : Builtin("_s_parse_")
        object StringOrd : Builtin("_s_ord_")
        object StringSubstring : Builtin("_s_substring_")
        object StringConcatenate : Builtin("_s_concatenate_")
        object StringEqual : Builtin("_s_equal_")
        object StringNeq : Builtin("_s_neq_")
        object StringLess : Builtin("_s_less_")
        object StringLeq : Builtin("_s_leq_")
        object StringGreater : Builtin("_s_greater_")
        object StringGeq : Builtin("_s_geq_")
    }
}
