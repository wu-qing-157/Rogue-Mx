package personal.wuqing.rogue.ir.grammar

sealed class IRStatement {
    sealed class Normal : IRStatement() {
        class Load(val dest: IRItem, val src: IRItem) : Normal()
        class Store(val src: IRItem, val dest: IRItem) : Normal()
        class Alloca(val item: IRItem) : Normal()
        class ICalc(val result: IRItem, val operator: IRCalcOp, val op1: IRItem, val op2: IRItem) : Normal()
        class ICmp(val result: IRItem, val operator: IRCmpOp, val op1: IRItem, val op2: IRItem) : Normal()
        class Call(val result: IRItem, val function: IRFunction, val args: List<IRItem>) : Normal()
        class Member(val result: IRItem, val base: IRItem, val index: Int) : Normal()
        class Index(val result: IRItem, val array: IRItem, val index: IRItem) : Normal()
        object Nop : Normal()
    }

    sealed class Terminate : IRStatement() {
        class Ret(val item: IRItem) : Terminate()
        class Branch(val cond: IRItem, val then: IRBlock, val els: IRBlock) : Terminate()
        class Jump(val dest: IRBlock) : Terminate()
    }

    class Phi(val result: IRItem, val list: List<Pair<IRItem, IRBlock>>) : IRStatement()
}
