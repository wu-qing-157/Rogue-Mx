package personal.wuqing.mxcompiler.llvm

sealed class LLVMStatement {
    interface Terminating

    class Ret(val type: LLVMType, val name: LLVMName?) : LLVMStatement(), Terminating {
        override fun toString() = "ret $type${name?.let { " $name" } ?: ""}"
    }

    class Load(val dest: LLVMName, val destType: LLVMType, val srcType: LLVMType, val src: LLVMName) : LLVMStatement() {
        override fun toString() = "$dest = load $destType, $srcType $src"
    }

    class Store(
        val src: LLVMName, val srcType: LLVMType, val dest: LLVMName, val destType: LLVMType
    ) : LLVMStatement() {
        override fun toString() = "store $srcType $src, $destType $dest"
    }

    class Alloca(val name: LLVMName, val type: LLVMType) : LLVMStatement() {
        override fun toString() = "$name = alloca $type"
    }

    class ICalc(
        val result: LLVMName, val operator: ICalcOperator, val type: LLVMType, val op1: LLVMName, val op2: LLVMName
    ) : LLVMStatement() {
        override fun toString() = "$result = $operator $type $op1, $op2"
    }

    class ICmp(
        val result: LLVMName, val operator: IComOperator, val type: LLVMType, val op1: LLVMName, val op2: LLVMName
    ) : LLVMStatement() {
        override fun toString() = "$result = icmp $operator $type $op1, $op2"
    }

    class Branch(
        val condType: LLVMType, val cond: LLVMName, val op1: LLVMName, val op2: LLVMName
    ) : LLVMStatement(), Terminating {
        override fun toString() = "br $condType $cond, label $op1, label $op2"
    }

    class Jump(val dest: LLVMName) : LLVMStatement(), Terminating {
        override fun toString() = "br label $dest"
    }

    class Phi(val name: LLVMName, val type: LLVMType, val list: List<Pair<LLVMName, LLVMName>>) : LLVMStatement() {
        override fun toString() = "$name = phi $type ${list.joinToString { (n, l) -> "[ $n, $l ]" }}"
    }

    class Call(
        val result: LLVMName?, val type: LLVMType, val name: LLVMName, val args: List<Pair<LLVMType, LLVMName>>
    ) : LLVMStatement() {
        override fun toString() =
            "${result?.let { "$result = " } ?: ""}call $type $name(${args.joinToString { (t, n) -> "$t $n" }})"
    }

    class Element(
        val result: LLVMName, val type: LLVMType,
        val srcType: LLVMType, val name: LLVMName, val indices: List<Pair<LLVMType, LLVMName>>
    ) : LLVMStatement() {
        override fun toString() =
            "$result = getelementptr $type, $srcType $name${indices.joinToString("") { (t, n) -> ", $t $n" }}"
    }
}
