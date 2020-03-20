package personal.wuqing.rogue.semantic.table

import personal.wuqing.rogue.grammar.MxType

object ClassTable {
    private val classes = mutableMapOf<String, MxType.Class>()
    operator fun contains(name: String) = name in classes
    operator fun get(name: String) =
        classes[name] ?: throw SymbolTableException.NotFoundException(
            name,
            "class"
        )

    operator fun set(name: String, clazz: MxType.Class) {
        if (name in classes) throw SymbolTableException.DuplicatedException(
            name,
            "class"
        )
        if (name in FunctionTable) throw SymbolTableException.DuplicatedException(
            name,
            "function"
        )
        if (name in VariableTable) throw SymbolTableException.DuplicatedException(
            name,
            "variable"
        )
        classes[name] = clazz
    }
}
