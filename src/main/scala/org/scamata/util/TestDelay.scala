// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import org.scamata.core._
import org.scamata.example.Toy4x4._
import org.scamata.solver.CentralizedSolver.negotiationSolver
import org.scamata.solver._

import scala.collection.SortedSet

/**
  * Companion object to test it
  */
object TestDelay extends App {
  val debug = false
  import org.scamata.example.AAMAS4x4._

  var allocation = new Allocation(pb)
  allocation = allocation.update(w1, SortedSet(t4))
  allocation = allocation.update(w2, SortedSet(t3))
  allocation = allocation.update(w3, SortedSet(t1))
  allocation = allocation.update(w4, SortedSet(t2))
  println(allocation)
  println(allocation.flowtime())
  println("w1 : "+allocation.delay(w1))
  println("w2 : "+allocation.delay(w2))

  val solver = new CentralizedSolver(pb, LC, SingleSwapAndSingleGift)//SingleSwapAndSingleGift or SingleSwapOnly or SingleGiftOnly
  solver.debug = true
  println(solver.reallocate(allocation).toString)
}