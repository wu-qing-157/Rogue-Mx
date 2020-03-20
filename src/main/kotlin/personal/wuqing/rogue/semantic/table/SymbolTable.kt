package personal.wuqing.rogue.semantic.table

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.grammar.MxType

object SymbolTable {
    private fun <E> MutableList<E>.removeLast() = removeAt(size - 1)

    private val thisFullList = mutableListOf<MxType.Class?>(null)
    private val thisList = mutableListOf<MxType.Class>()
    private val loopList = mutableListOf<ASTNode.Statement.Loop>()
    private val functionList = mutableListOf<ASTNode.Declaration.Function>()
    val thisType get() = thisList.lastOrNull()
    val loop get() = loopList.lastOrNull()
    private val function get() = functionList.lastOrNull()
    val returnType get() = function?.returnType

    fun new(thisType: MxType.Class? = null) {
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
