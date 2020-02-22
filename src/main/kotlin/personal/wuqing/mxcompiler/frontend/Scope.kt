package personal.wuqing.mxcompiler.frontend


class Scope private constructor(
    private val parent: Scope?,
    private val variables: MutableMap<String, VariableOrType>,
    private val functions: MutableMap<FunctionDefinition, Function>
) {

    class VariableOrType private constructor(val variable: Variable?, val type: Type?) {
        constructor(variable: Variable) : this(variable, null)
        constructor(type: Type) : this(null, type)
    }

    constructor(parent: Scope) : this(parent, mutableMapOf(), mutableMapOf())

    companion object {
        val TOP = Scope(
            parent = null, functions = mutableMapOf(), variables = mutableMapOf(
                "int" to VariableOrType(IntType),
                "bool" to VariableOrType(BoolType),
                "string" to VariableOrType(StringType),
                "void" to VariableOrType(VoidType)
            )
        )
    }

    class DuplicationException constructor(val description: String) : Exception() {
        constructor(definition: FunctionDefinition) : this(definition.toString())
    }

    operator fun get(name: String): VariableOrType? = variables[name] ?: parent?.get(name)
    operator fun get(definition: FunctionDefinition): Function? = functions[definition] ?: parent?.get(definition)

    operator fun set(name: String, variable: Variable) =
        if (name in variables) throw DuplicationException(name)
        else variables[name] = VariableOrType(variable)

    operator fun set(name: String, type: Type) =
        if (name in variables) throw DuplicationException(name)
        else variables[name] = VariableOrType(type)

    operator fun set(definition: FunctionDefinition, function: Function) =
        if (definition in functions) throw DuplicationException(definition)
        else functions[definition] = function
}
