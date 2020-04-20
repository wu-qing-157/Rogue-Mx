package personal.wuqing.rogue.ir.grammar

sealed class IRStatement {
    sealed class Normal : IRStatement() {
        class Load(val dest: IRItem.Local, val src: IRItem) : Normal() {
            override fun toString() = "$dest = load $src"
        }

        class Store(val src: IRItem, val dest: IRItem) : Normal() {
            override fun toString() = "store $src to $dest"
        }

        class Alloca(val item: IRItem.Local) : Normal() {
            override fun toString() = "$item = alloca"
        }

        class ICalc(
            val result: IRItem.Local, val operator: IRCalcOp, val op1: IRItem, val op2: IRItem
        ) : Normal() {
            override fun toString() = "$result = $op1 $operator $op2"
        }

        class ICmp(val result: IRItem.Local, val operator: IRCmpOp, val op1: IRItem, val op2: IRItem) : Normal() {
            override fun toString() = "$result = $op1 $operator $op2"
        }

        class Call(val result: IRItem.Local?, val function: IRFunction, val args: List<IRItem>) : Normal() {
            override fun toString() = result?.let {
                "$result = $function(${args.joinToString()})"
            } ?: "$function(${args.joinToString()})"
        }

        object NOP : Normal()
    }

    sealed class Terminate : IRStatement() {
        class Ret(val item: IRItem?) : Terminate() {
            override fun toString() = item?.let { "return $item" } ?: "return"
        }

        class Branch(val cond: IRItem, val then: IRBlock, val els: IRBlock) : Terminate() {
            override fun toString() = "branch $cond (true -> $then) (false -> $els)"
        }

        class Jump(val dest: IRBlock) : Terminate() {
            override fun toString() = "goto $dest"
        }
    }

    class Phi(val result: IRItem.Local, val list: Map<IRBlock, IRItem>) : IRStatement() {
        override fun toString() =
            "$result = phi ${list.entries.joinToString(" ") { (block, item) -> "($block -> $item)" }}"
    }
}
