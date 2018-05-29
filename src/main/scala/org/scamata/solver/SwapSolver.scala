// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.deal._

import scala.collection.SortedSet

/**
  * Minimizing the rule by applying single swap
  * @param pb to be solver
  * @param rule to be optimized
  */
class SwapSolver(pb : MWTA, rule : SocialRule) extends Solver(pb, rule) {

  /**
    * Returns a random allocation with no more improving apply
    */
  override protected def solve(): Allocation = {
    Allocation.randomAllocation(pb)//TODO
  }

}

/**
  * Companion object to test it
  */
object SwapSolver extends App {
  val debug = false
  import org.scamata.example.toy4x4._
  println(pb)
  val negotiationSolver = new SwapSolver(pb,Flowtime)
  println(negotiationSolver.run().toString)
}