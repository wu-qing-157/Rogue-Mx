package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.grammar.MxVariable

sealed class IRStatement {
    sealed class Normal : IRStatement() {
        class Load(val dest: IRItem, val src: IRItem) : Normal()
        class Store(val src: IRItem, val dest: IRItem) : Normal()
        class Alloca(val item: IRItem) : Normal()
        class MallocObject(val item: IRItem) : Normal() {
            val size = (item.type as? IRType.Class ?: error("unexpected class type")).members.size
        }
        class MallocArray(val item: IRItem, val length: IRItem) : Normal() {
            val single = (item.type as? IRType.Array ?: error("unexpected array type")).base.size
        }
        class ICalc(val result: IRItem, val operator: IRCalcOp, val op1: IRItem, val op2: IRItem) : Normal()
        class ICmp(val result: IRItem, val operator: IRCmpOp, val op1: IRItem, val op2: IRItem) : Normal()
        class Call(val result: IRItem, val function: IRFunction, val args: List<IRItem>) : Normal()
        class Member(val result: IRItem, val base: IRItem, val variable: MxVariable) : Normal() {
            val index = (base.type as? IRType.Class ?: error("access member of non-class type")).members.index[variable]
        }
        class Index(val result: IRItem, val array: IRItem, val index: IRItem) : Normal()
        class Size(val result: IRItem, val base: IRItem) : Normal()
        object Nop : Normal()
    }

    sealed class Terminate : IRStatement() {
        class Ret(val item: IRItem?) : Terminate()
        class Branch(val cond: IRItem, val then: IRBlock, val els: IRBlock) : Terminate()
        class Jump(val dest: IRBlock) : Terminate()
    }

    class Phi(val result: IRItem, val list: List<Pair<IRItem, IRBlock>>) : IRStatement()
}
