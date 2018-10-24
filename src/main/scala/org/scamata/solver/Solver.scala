// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.MATA
import org.scamata.core.Allocation

/**
  * Class representing a social rule
  */
class SocialRule{
  override def toString: String = this match{
    case LCmax => "cmax"
    case LF => "flowtime"
    case LC => "cost"
  }
}
case object LF extends SocialRule
case object LCmax extends SocialRule
case object LC extends SocialRule

/**
  * Abstract class representing a solver
  * @param pb to be solver
  */
abstract class Solver(val pb : MATA, val rule : SocialRule) {
  var debug = false

  var solvingTime : Long = 0

  /**
    * Returns an allocation
    */
  protected def solve() : Allocation

  /**
    * Returns an allocation and update solving time
    */
  def run() : Allocation = {
    val startingTime = System.nanoTime()
    val allocation = solve()
    solvingTime = System.nanoTime() - startingTime
    if (rule == SingleSwapOnly && ! allocation.isSingle) throw new RuntimeException(s"Solver: the outcome\n $allocation\n for\n ${allocation.pb} assign more than one task per agent")
    if (allocation.isCoherent && allocation.isSound) allocation
    else throw new RuntimeException(s"Solver: the outcome\n $allocation\nis not coherent or sound for\n ${allocation.pb}")
  }

}
