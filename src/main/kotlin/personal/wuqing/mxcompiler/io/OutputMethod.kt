package personal.wuqing.mxcompiler.io

import java.io.FileOutputStream
import java.io.FileWriter
import java.io.ObjectOutputStream

sealed class OutputMethod {
    abstract operator fun invoke(string: String)
    abstract operator fun invoke(target: Any)

    object Stdout : OutputMethod() {
        override fun invoke(string: String) = println(string)
        override fun invoke(target: Any) = ObjectOutputStream(System.out).use {
            it.writeObject(target)
        }
    }

    class File(private val name: String) : OutputMethod() {
        override fun invoke(string: String) = FileWriter(name).use {
            it.write(string)
        }

        override fun invoke(target: Any) = ObjectOutputStream(FileOutputStream(name)).use {
            it.writeObject(target)
        }
    }
}
