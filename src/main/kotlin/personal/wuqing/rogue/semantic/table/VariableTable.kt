package personal.wuqing.rogue.semantic.table

import personal.wuqing.rogue.grammar.MxVariable

object VariableTable {
    private fun <E> MutableList<E>.removeLast() = removeAt(size - 1)

    private val definitionIndexed = mutableMapOf<String, MutableList<MxVariable>>()
    private val levelIndexed = mutableListOf(mutableListOf<String>())
    operator fun contains(name: String) = name in levelIndexed.last()
    operator fun get(name: String) =
        definitionIndexed[name]?.lastOrNull() ?: throw SymbolTableException.NotFoundException(name, "variable")

    operator fun set(name: String, variable: MxVariable) {
        if (name in levelIndexed.last()) throw SymbolTableException.DuplicatedException(name, "variable")
        // if (name in FunctionTable) throw SymbolTableException.DuplicatedException(name, "function")
        if (name in ClassTable) throw SymbolTableException.DuplicatedException(name, "class")
        definitionIndexed.putIfAbsent(name, mutableListOf())
        definitionIndexed[name]!! += variable
        levelIndexed.last() += name
    }

    fun new() {
        levelIndexed += mutableListOf<String>()
    }

    fun drop() {
        for (definition in levelIndexed.last()) definitionIndexed[definition]!!.removeLast()
        levelIndexed.removeLast()
    }
}
