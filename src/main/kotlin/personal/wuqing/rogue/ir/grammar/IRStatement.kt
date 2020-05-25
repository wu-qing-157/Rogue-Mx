package personal.wuqing.rogue.ir.grammar

sealed class IRStatement {
    abstract val use: Iterable<IRItem>
    abstract val all: Iterable<IRItem.Local>

    interface WithResult {
        val result: IRItem.Local?
    }

    sealed class Normal : IRStatement() {
        abstract fun transform(map: Map<IRItem.Local, IRItem>): Normal
        abstract fun transUse(map: Map<IRItem, IRItem>): Normal

        class Load(val dest: IRItem.Local, val src: IRItem) : Normal(), WithResult {
            override val use = listOf(src)
            override val result = dest
            override val all = listOf(dest, src).filterIsInstance<IRItem.Local>()
            override fun toString() = "$dest = load $src"
            override fun transform(map: Map<IRItem.Local, IRItem>) =
                Load(map[dest] as IRItem.Local? ?: dest, map[src] ?: src)

            override fun transUse(map: Map<IRItem, IRItem>) = Load(dest, map[src] ?: src)
        }

        class Store(val src: IRItem, val dest: IRItem) : Normal() {
            override val use = listOf(src, dest)
            override val all = listOf(src, dest).filterIsInstance<IRItem.Local>()
            override fun toString() = "store $src to $dest"
            override fun transform(map: Map<IRItem.Local, IRItem>) = Store(map[src] ?: src, map[dest] ?: dest)
            override fun transUse(map: Map<IRItem, IRItem>) = Store(map[src] ?: src, map[dest] ?: dest)
        }

        class Alloca(val item: IRItem.Local) : Normal() {
            override val use = listOf<IRItem>()
            override val all get() = error("analyzing alloca")
            override fun toString() = "$item = alloca"
            override fun transform(map: Map<IRItem.Local, IRItem>) = error("transforming alloca")
            override fun transUse(map: Map<IRItem, IRItem>) = error("transforming alloca")
        }

        class ICalc(
            override val result: IRItem.Local, val operator: IRCalcOp, val op1: IRItem, val op2: IRItem
        ) : Normal(), WithResult {
            override val use = listOf(op1, op2)
            override val all = listOf(result, op1, op2).filterIsInstance<IRItem.Local>()
            override fun toString() = "$result = $op1 $operator $op2"
            override fun transform(map: Map<IRItem.Local, IRItem>) =
                ICalc(map[result] as IRItem.Local? ?: result, operator, map[op1] ?: op1, map[op2] ?: op2)

            override fun transUse(map: Map<IRItem, IRItem>) = ICalc(result, operator, map[op1] ?: op1, map[op2] ?: op2)
        }

        class ICmp(
            override val result: IRItem.Local, val operator: IRCmpOp, val op1: IRItem, val op2: IRItem
        ) : Normal(), WithResult {
            override val use = listOf(op1, op2)
            override val all = listOf(result, op1, op2).filterIsInstance<IRItem.Local>()
            override fun toString() = "$result = $op1 $operator $op2"
            override fun transform(map: Map<IRItem.Local, IRItem>) =
                ICmp(map[result] as IRItem.Local? ?: result, operator, map[op1] ?: op1, map[op2] ?: op2)

            override fun transUse(map: Map<IRItem, IRItem>) = ICmp(result, operator, map[op1] ?: op1, map[op2] ?: op2)
        }

        class Call(
            override val result: IRItem.Local?, val function: IRFunction, val args: List<IRItem>
        ) : Normal(), WithResult {
            override val use = args
            override val all = (args + result).filterIsInstance<IRItem.Local>()
            override fun toString() = result?.let {
                "$result = $function(${args.joinToString()})"
            } ?: "$function(${args.joinToString()})"

            override fun transform(map: Map<IRItem.Local, IRItem>) =
                Call(map[result] as IRItem.Local? ?: result, function, args.map { map[it] ?: it })

            override fun transUse(map: Map<IRItem, IRItem>) = Call(result, function, args.map { map[it] ?: it })
        }

        object NOP : Normal() {
            override val use = listOf<IRItem>()
            override val all get() = error("analyzing NOP")
            override fun transform(map: Map<IRItem.Local, IRItem>) = error("transforming NOP")
            override fun transUse(map: Map<IRItem, IRItem>) = error("transforming NOP")
        }
    }

    sealed class Terminate : IRStatement() {
        abstract fun transform(map: Map<IRItem.Local, IRItem>): Terminate
        abstract fun translate(map: Map<IRBlock, IRBlock>): Terminate
        abstract fun transUse(map: Map<IRItem, IRItem>): Terminate

        class Ret(val item: IRItem?) : Terminate() {
            override val use = item?.let { listOf(it) } ?: listOf()
            override val all = listOf(item).filterIsInstance<IRItem.Local>()
            override fun toString() = item?.let { "return $item" } ?: "return"
            override fun transform(map: Map<IRItem.Local, IRItem>) = Ret(map[item] ?: item)
            override fun transUse(map: Map<IRItem, IRItem>) = Ret(map[item] ?: item)
            override fun translate(map: Map<IRBlock, IRBlock>) = this
        }

        class Branch(val cond: IRItem, val then: IRBlock, val els: IRBlock) : Terminate() {
            override val use = listOf(cond)
            override val all = listOf(cond).filterIsInstance<IRItem.Local>()
            override fun toString() = "branch $cond (true -> $then) (false -> $els)"
            override fun transform(map: Map<IRItem.Local, IRItem>) = Branch(map[cond] ?: cond, then, els)
            override fun transUse(map: Map<IRItem, IRItem>) = Branch(map[cond] ?: cond, then, els)
            override fun translate(map: Map<IRBlock, IRBlock>) = Branch(cond, map[then] ?: then, map[els] ?: els)
        }

        class Jump(val dest: IRBlock) : Terminate() {
            override val use = listOf<IRItem>()
            override val all = listOf<IRItem.Local>()
            override fun toString() = "goto $dest"
            override fun transform(map: Map<IRItem.Local, IRItem>) = this
            override fun transUse(map: Map<IRItem, IRItem>) = this
            override fun translate(map: Map<IRBlock, IRBlock>) = Jump(map[dest] ?: dest)
        }
    }

    class Phi(override val result: IRItem.Local, val list: Map<IRBlock, IRItem>) : IRStatement(), WithResult {
        override val use = list.values
        override val all = (list.values + result).filterIsInstance<IRItem.Local>()
        override fun toString() =
            "$result = phi ${list.entries.joinToString(" ") { (block, item) -> "($block -> $item)" }}"

        fun transform(map: Map<IRItem.Local, IRItem>) =
            Phi(map[result] as IRItem.Local? ?: result, list.mapValues { (_, it) -> map[it] ?: it })

        fun transUse(map: Map<IRItem, IRItem>) = Phi(result, list.mapValues { (_, it) -> map[it] ?: it })
        fun translate(map: Map<IRBlock, IRBlock>) = Phi(result, list.mapKeys { (it, _) -> map[it] ?: it })
    }
}
