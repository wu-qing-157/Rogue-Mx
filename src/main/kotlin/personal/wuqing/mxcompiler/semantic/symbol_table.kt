package personal.wuqing.mxcompiler.semantic

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.grammar.Function
import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.grammar.Variable

fun <E> MutableList<E>.removeLast() = removeAt(size - 1)

object FunctionTable {
    private val functions = mutableMapOf<String, Function>()
    operator fun contains(name: String) = name in functions
    operator fun get(name: String) =
        functions[name] ?: throw SymbolTableException.NotFoundException(name, "function")

    operator fun set(name: String, function: Function) {
        if (name in functions) throw SymbolTableException.DuplicatedException(name, "function")
        if (name in ClassTable) throw SymbolTableException.DuplicatedException(name, "class")
        if (name in VariableTable) throw SymbolTableException.DuplicatedException(name, "variable")
        functions[name] = function
    }
}

object ClassTable {
    private val classes = mutableMapOf<String, Type.Class>()
    operator fun contains(name: String) = name in classes
    operator fun get(name: String) =
        classes[name] ?: throw SymbolTableException.NotFoundException(name, "class")

    operator fun set(name: String, clazz: Type.Class) {
        if (name in classes) throw SymbolTableException.DuplicatedException(name, "class")
        if (name in FunctionTable) throw SymbolTableException.DuplicatedException(name, "function")
        if (name in VariableTable) throw SymbolTableException.DuplicatedException(name, "variable")
        classes[name] = clazz
    }
}

object VariableTable {
    private val definitionIndexed = mutableMapOf<String, MutableList<Variable>>()
    private val levelIndexed = mutableListOf(mutableListOf<String>())
    operator fun contains(name: String) = name in levelIndexed.last()
    operator fun get(name: String) =
        definitionIndexed[name]?.lastOrNull() ?: throw SymbolTableException.NotFoundException(name, "variable")

    operator fun set(name: String, variable: Variable) {
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

sealed class SymbolTableException(message: String) : Exception(message) {
    class NotFoundException(definition: String, item: String) :
        SymbolTableException("\"$definition\" cannot be resolved as a $item")

    class DuplicatedException(definition: String, item: String) :
        SymbolTableException("\"$definition\" has already been defined as a $item")
}

object SymbolTable {
    private val thisFullList = mutableListOf<Type.Class?>(null)
    private val thisList = mutableListOf<Type.Class>()
    private val loopList = mutableListOf<ASTNode.Statement.Loop>()
    private val functionList = mutableListOf<ASTNode.Declaration.Function>()
    val thisType get() = thisList.lastOrNull()
    val loop get() = loopList.lastOrNull()
    private val function get() = functionList.lastOrNull()
    val returnType get() = function?.returnType

    fun new(thisType: Type.Class? = null) {
        VariableTable.new()
        thisFullList += thisType?.also { thisList += it }
    }

    fun drop() {
        VariableTable.drop()
        thisFullList.last()?.also { thisList.removeLast() }
        thisFullList.removeLast()
    }

    fun newLoop(loop: ASTNode.Statement.Loop) {
        loopList += loop
    }

    fun dropLoop() {
        loopList.removeLast()
    }

    fun newFunction(function: ASTNode.Declaration.Function) {
        functionList += function
    }

    fun dropFunction() {
        functionList.removeLast()
    }
}
