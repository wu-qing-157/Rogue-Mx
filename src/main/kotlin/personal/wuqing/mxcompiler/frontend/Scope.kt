package personal.wuqing.mxcompiler.frontend

class Scope private constructor(
    private val namespace: MutableList<String>,
    variables: Map<String, Variable>,
    functions: Map<FunctionDefinition, Function>) {

    private val variables = HashMap(variables)
    private val functions = HashMap(functions)

    constructor(base: Scope, name: String) : this(base.namespace.apply { add(name) }, base.variables, base.functions)

    fun put(name: String, variable: Variable) {
        variables[name] = variable
    }

    fun put(name: FunctionDefinition, function: Function) {
        functions[name] = function
    }
}
