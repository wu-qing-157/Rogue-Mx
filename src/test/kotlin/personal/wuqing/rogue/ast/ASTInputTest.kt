package personal.wuqing.rogue.ast

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream

const val ast = "test/104.ast"
const val description = "test/104.ast.test"

fun main() {
    val input = FileInputStream(ast)
    val objectInput = ObjectInputStream(input)
    val root = objectInput.readObject() as ASTNode
    val result = ASTPrinter.summary(root).toByteArray()
    val output = FileOutputStream(description)
    output.write(result)
    output.close()
}
