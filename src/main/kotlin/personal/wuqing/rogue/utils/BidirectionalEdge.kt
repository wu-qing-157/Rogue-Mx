package personal.wuqing.rogue.utils

abstract class BidirectionalEdge<T>(val a: T, val b: T) {
    operator fun component1() = a
    operator fun component2() = b
    operator fun contains(o: T) = a == o || b == o
    override fun toString() = "($a $b)"
    override fun hashCode() = a.hashCode() xor b.hashCode()
    override fun equals(other: Any?) =
        other is BidirectionalEdge<*> && ((a == other.a && b == other.b) || (a == other.b && b == other.a))
}
