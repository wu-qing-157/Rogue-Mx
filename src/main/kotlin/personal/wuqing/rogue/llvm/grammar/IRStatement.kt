package personal.wuqing.rogue.llvm.grammar

sealed class IRStatement {
    interface Terminating

    class Ret(val item: IRItem) : IRStatement(), Terminating {
        override fun toString() = when (item) {
            is IRItem.Void -> "ret void"
            else -> "ret ${item.type} $item"
        }
    }

    class Load(val dest: IRItem, val src: IRItem) : IRStatement() {
        init {
            assert(src.type is IRType.Pointer && src.type.base == dest)
        }

        override fun toString() = "$dest = load ${dest.type}, ${src.type} $src"
    }

    class Store(val src: IRItem, val dest: IRItem) : IRStatement() {
        init {
            assert(dest.type is IRType.Pointer && dest.type.base == src)
        }

        override fun toString() = "store ${src.type} $src, ${dest.type} $dest"
    }

    class Alloca(val item: IRItem) : IRStatement() {
        init {
            assert(item.type is IRType.Pointer)
        }

        override fun toString() = "$item = alloca ${(item.type as IRType.Pointer).base}"
    }

    class ICalc(val result: IRItem, val operator: IRCalcOp, val op1: IRItem, val op2: IRItem) : IRStatement() {
        init {
            assert(result.type == op1.type && result.type == op2.type)
        }

        override fun toString() = "$result = $operator ${result.type} $op1, $op2"
    }

    class ICmp(val result: IRItem, val operator: IRCmpOp, val op1: IRItem, val op2: IRItem) : IRStatement() {
        init {
            assert(result.type == IRType.I1 && op1.type == op2.type)
        }

        override fun toString() = "$result = icmp $operator ${op1.type} $op1, $op2"
    }

    class Branch(val cond: IRItem, val then: IRBlock, val els: IRBlock) : IRStatement(), Terminating {
        init {
            assert(cond.type == IRType.I1)
        }

        override fun toString() = "br ${cond.type} $cond, label $then, label $els"
    }

    class Jump(val dest: IRBlock) : IRStatement(), Terminating {
        override fun toString() = "br label $dest"
    }

    class Phi(val result: IRItem, val list: List<Pair<IRItem, IRBlock>>) : IRStatement() {
        init {
            assert(list.all { it.first.type == result.type })
        }

        override fun toString() = "$result = phi ${result.type} ${list.joinToString { (n, l) -> "[ $n, $l ]" }}"
    }

    class Call(val result: IRItem, val function: IRFunction, val args: List<IRItem>) : IRStatement() {
        init {
            assert(result.type == function.ret && function.args == args.map { it.type })
        }

        override fun toString() = when (result) {
            is IRItem.Void -> "call void @${function.name}(${args.joinToString { (t, d) -> "$t $d" }})"
            else -> "$result = call ${result.type} @${function.name}(${args.joinToString { (t, d) -> "$t $d" }})"
        }
    }

    class Cast(val from: IRItem, val to: IRItem) : IRStatement() {
        override fun toString() = "$to = bitcast ${from.type} $from to ${to.type}"
    }

    class Element(val result: IRItem, val src: IRItem, val indices: List<IRItem>) : IRStatement() {
        override fun toString() =
            "$result = getelementptr ${(src.type as IRType.Pointer).base}, ${src.type} $src, ${indices.joinToString { (t, n) -> "$t $n" }}"
    }
}
