// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.MWTA
import org.scamata.core.Allocation

/**
  * Class representing a social rule
  */
class SocialRule{
  override def toString: String = this match{
    case LCmax => "cmax"
    case LC => "flowtime"
  }
}
case object LC extends SocialRule
case object LCmax extends SocialRule

/**
  * Abstract class representing a solver
  * @param pb to be solver
  */
abstract class Solver(val pb : MWTA, val rule : SocialRule) {
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
    if (allocation.isSound) allocation
    else throw new RuntimeException(s"Solver: the outcome\n $allocation\nis not complete for\n ${allocation.pb}")
  }

}
