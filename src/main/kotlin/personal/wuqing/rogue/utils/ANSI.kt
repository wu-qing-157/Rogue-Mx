package personal.wuqing.rogue.utils

object ANSI {
    fun red(s: String) = "\u001B[31m$s\u001B[0m"
    fun yellow(s: String) = "\u001B[33m$s\u001B[0m"
    fun bold(s: String) = "\u001B[1m$s\u001B[0m"
}
