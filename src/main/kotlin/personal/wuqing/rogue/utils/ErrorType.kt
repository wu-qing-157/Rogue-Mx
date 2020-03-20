package personal.wuqing.rogue.utils

sealed class ErrorType {
    object Fatal : ErrorType() {
        override fun toString() = ANSI.red("fatal error:")
    }

    object Unsupported : ErrorType() {
        override fun toString() = ANSI.red("unsupported:")
    }

    object Error : ErrorType() {
        override fun toString() = ANSI.red("error:")
    }

    object Warning : ErrorType() {
        override fun toString() = ANSI.yellow("warning:")
    }

    object Info : ErrorType() {
        override fun toString() = ANSI.bold("info:")
    }
}
