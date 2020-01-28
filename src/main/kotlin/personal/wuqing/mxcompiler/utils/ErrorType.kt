package personal.wuqing.mxcompiler.utils

object FatalError {
    override fun toString() = ANSI.red("fatal error:")
}

object Unsupported {
    override fun toString() = ANSI.red("unsupported:")
}

object Error {
    override fun toString() = ANSI.red("error:")
}

object Warning {
    override fun toString() = ANSI.yellow("warning:")
}

object Info {
    override fun toString() = ANSI.bold("info:")
}
