package personal.wuqing.mxcompiler.utils

object FatalError {
    override fun toString() = "\u001B[31mfatal error:\u001B[0m"
}

object Unsupported {
    override fun toString() = "\u001B[31munsupported:\u001B[0m"
}

object Error {
    override fun toString() = "\u001B[31merror:\u001B[0m"
}

object Warning {
    override fun toString() = "\u001B[33mwarning:\u001B[0m"
}

object Info {
    override fun toString() = "\u001B[37;1minfo:\u001B[0m"
}
