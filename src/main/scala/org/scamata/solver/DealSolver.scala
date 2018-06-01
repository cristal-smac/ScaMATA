// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}


/**
  * Class representing a strategy
  */
class DealStrategy{
  override def toString: String = this match{
    case SingleGiftOnly => "SingleGiftOnly"
    case SingleSwapAndSingleGift => "SingleSwapAndSingleGift"
  }
}
case object SingleGiftOnly extends DealStrategy
case object SingleSwapAndSingleGift extends DealStrategy


/**
  * Abstract solver based on deals (gifts/swaps)
  * @param pb to be solver
  * @param rule
  */
abstract class DealSolver(pb : MWTA, rule : SocialRule) extends Solver(pb, rule){
  var nbPropose = 0
  var nbAccept = 0
  var nbReject = 0
  var nbWithdraw = 0
  var nbConfirm = 0
  var nbInform = 0
  var nbCancel = 0

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    val allocation = Allocation.randomAllocation(pb)
    if (debug) println(s"Give with a random allocation:\n$allocation")
    reallocate(allocation)
  }

  /**
    * Modify the current allocation
    */
  def reallocate(allocation: Allocation) : Allocation

}
