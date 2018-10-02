// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}


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
case object SingleSwapOnly extends DealStrategy

/**
  * Abstract solver based on deals (gifts/swaps)
  * @param pb to be solver
  * @param rule
  * @param strategy for selecting the kind of deal
  */
abstract class DealSolver(pb : MATA, rule : SocialRule, strategy : DealStrategy) extends Solver(pb, rule){
  var nbPropose = 0
  var nbCounterPropose = 0
  var nbAccept = 0
  var nbReject = 0
  var nbWithdraw = 0
  var nbConfirmGift = 0
  var nbConfirmSwap = 0
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
