package personal.wuqing.rogue.utils

interface DirectionalNodeWithPrev<T> {
    val next: Iterable<T>
    val prev: Iterable<T>
}
