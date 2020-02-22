package personal.wuqing.mxcompiler.utils

import java.io.FileOutputStream
import java.io.FileWriter
import java.io.ObjectOutputStream

sealed class OutputMethod {
    abstract fun output(string: String)
    abstract fun output(target: Any)
}

object StdoutOutput : OutputMethod() {
    override fun output(string: String) = println(string)
    override fun output(target: Any) = ObjectOutputStream(System.out).use {
        it.writeObject(target)
    }
}

class FileOutput(private val name: String) : OutputMethod() {
    override fun output(string: String) = FileWriter(name).use {
        it.write(string)
    }

    override fun output(target: Any) = ObjectOutputStream(FileOutputStream(name)).use {
        it.writeObject(target)
    }
}
