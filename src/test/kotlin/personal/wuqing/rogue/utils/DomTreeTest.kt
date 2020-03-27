package personal.wuqing.rogue.utils

class TextNode(private val name: String) : DirectionalNodeWithPrev<TextNode> {
    override val next = mutableListOf<TextNode>()
    override val prev = mutableListOf<TextNode>()
    override fun toString() = name
}

fun main() {
    val a = TextNode("a")
    val b = TextNode("b")
    val c = TextNode("c")
    val d = TextNode("d")
    val e = TextNode("e")
    val f = TextNode("f")
    val g = TextNode("g")
    val h = TextNode("h")
    val i = TextNode("i")
    val j = TextNode("j")
    val k = TextNode("k")
    val l = TextNode("l")
    val m = TextNode("m")
    fun link(a: TextNode, b: TextNode) {
        a.next += b
        b.prev += a
    }
    link(a, b)
    link(a, c)
    link(b, d)
    link(b, g)
    link(c, e)
    link(c, h)
    link(d, f)
    link(d, g)
    link(e, h)
    link(e, c)
    link(f, i)
    link(f, k)
    link(g, j)
    link(h, m)
    link(i, l)
    link(j, i)
    link(k, l)
    link(l, b)
    link(l, m)
    val tree = DomTree(a)
    println(tree.child)
}
