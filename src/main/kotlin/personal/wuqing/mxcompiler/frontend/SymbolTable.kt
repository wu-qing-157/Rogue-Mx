package personal.wuqing.mxcompiler.frontend

fun <E> MutableList<E>.removeLast() = removeAt(size - 1)

object FunctionTable {
    private val definitionIndexed = mutableMapOf<FunctionDefinition, MutableList<Function>>()
    private val levelIndexed = mutableListOf(mutableListOf<FunctionDefinition>())
    operator fun get(definition: FunctionDefinition) =
        definitionIndexed[definition]?.lastOrNull() ?: throw SymbolTable.NotFoundException()
    operator fun set(definition: FunctionDefinition, function: Function) {
        if (definition in levelIndexed.last()) throw SymbolTable.DuplicatedException()
        definitionIndexed.putIfAbsent(definition, mutableListOf())
        definitionIndexed[definition]!! += function
        levelIndexed.last() += definition
    }

    fun new() {
        levelIndexed += mutableListOf<FunctionDefinition>()
    }
    fun drop() {
        for (definition in levelIndexed.last()) definitionIndexed[definition]!!.removeLast()
        levelIndexed.removeLast()
    }
}

object ClassTable {
    private val definitionIndexed = mutableMapOf<String, MutableList<ClassType>>()
    private val levelIndexed = mutableListOf(mutableListOf<String>())
    operator fun get(definition: String) =
        definitionIndexed[definition]?.lastOrNull() ?: throw SymbolTable.NotFoundException()
    operator fun set(definition: String, clazz: ClassType) {
        if (definition in levelIndexed.last()) throw SymbolTable.DuplicatedException()
        definitionIndexed.putIfAbsent(definition, mutableListOf())
        definitionIndexed[definition]!! += clazz
        levelIndexed.last() += definition
    }

    fun new() {
        levelIndexed += mutableListOf<String>()
    }
    fun drop() {
        for (definition in levelIndexed.last()) definitionIndexed[definition]!!.removeLast()
        levelIndexed.removeLast()
    }
}

object VariableTable {
    private val definitionIndexed = mutableMapOf<String, MutableList<Variable>>()
    private val levelIndexed = mutableListOf(mutableListOf<String>())
    operator fun get(definition: String) =
        definitionIndexed[definition]?.lastOrNull() ?: throw SymbolTable.NotFoundException()
    operator fun set(definition: String, variable: Variable) {
        if (definition in levelIndexed.last()) throw SymbolTable.DuplicatedException()
        definitionIndexed.putIfAbsent(definition, mutableListOf())
        definitionIndexed[definition]!! += variable
        levelIndexed.last() += definition
    }

    fun new() {
        levelIndexed += mutableListOf<String>()
    }
    fun drop() {
        for (definition in levelIndexed.last()) definitionIndexed[definition]!!.removeLast()
        levelIndexed.removeLast()
    }
}

object SymbolTable {
    class NotFoundException : Exception()
    class DuplicatedException : Exception()
    private val thisType = mutableListOf<Type>(UnknownType)
    fun thisType() = thisType.findLast { it != UnknownType } ?: UnknownType
    fun new(thisType: Type = UnknownType) {
        FunctionTable.new()
        ClassTable.new()
        VariableTable.new()
        this.thisType += thisType
    }
    fun drop() {
        FunctionTable.drop()
        ClassTable.drop()
        VariableTable.drop()
        thisType.removeLast()
    }
}
