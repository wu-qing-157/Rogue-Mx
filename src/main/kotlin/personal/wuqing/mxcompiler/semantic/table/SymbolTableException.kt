package personal.wuqing.mxcompiler.semantic.table

sealed class SymbolTableException(message: String) : Exception(message) {
    class NotFoundException(definition: String, item: String) :
        SymbolTableException("\"$definition\" cannot be resolved as a $item")

    class DuplicatedException(definition: String, item: String) :
        SymbolTableException("\"$definition\" has already been defined as a $item")
}
