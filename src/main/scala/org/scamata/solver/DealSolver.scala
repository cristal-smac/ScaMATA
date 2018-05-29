// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}

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
