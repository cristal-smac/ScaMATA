// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.MATA
import org.scamata.core.Allocation

/**
  * Class representing a social rule
  */
class SocialRule{
  /**
    * Returns a string representation of the social rule
    */
  override def toString: String = this match{
    case Cmax => "cmax"
    case Flowtime => "flowtime"
  }
}
case object Flowtime extends SocialRule
case object Cmax extends SocialRule

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
    if (allocation.isComplete()) allocation
    else throw new RuntimeException("Solver: the outcome is not complete")
  }

}
