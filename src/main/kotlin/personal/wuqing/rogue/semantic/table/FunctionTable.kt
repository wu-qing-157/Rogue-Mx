package personal.wuqing.rogue.semantic.table

import personal.wuqing.rogue.grammar.MxFunction

object FunctionTable {
    private val functions = mutableMapOf<String, MxFunction>()
    operator fun contains(name: String) = name in functions
    operator fun get(name: String) =
        functions[name] ?: throw SymbolTableException.NotFoundException(
            name,
            "function"
        )

    operator fun set(name: String, function: MxFunction) {
        if (name in functions) throw SymbolTableException.DuplicatedException(
            name,
            "function"
        )
        if (name in ClassTable) throw SymbolTableException.DuplicatedException(
            name,
            "class"
        )
        if (name in VariableTable) throw SymbolTableException.DuplicatedException(
            name,
            "variable"
        )
        functions[name] = function
    }
}
