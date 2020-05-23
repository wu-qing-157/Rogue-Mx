package personal.wuqing.rogue.riscv.translator

import personal.wuqing.rogue.riscv.grammar.RVRegister
import personal.wuqing.rogue.utils.BidirectionalEdge

class RegisterEdge(a: RVRegister, b: RVRegister) : BidirectionalEdge<RVRegister>(a, b)
