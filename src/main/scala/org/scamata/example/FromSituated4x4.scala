// Copyright (C) Maxime MORGE 2018
package org.scamata.example

import org.scamata.core.{Allocation, MATA, Task, Agent}
import org.scamata.solver.{CentralizedSolver, LCmax, LF, SingleGiftOnly, SingleSwapAndSingleGift}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */

object FromSituated4x4{
  val w1 = new Agent("1")
  val w2 = new Agent("2")
  val w3 = new Agent("3")
  val w4 = new Agent("4")
  val workers = SortedSet(w1, w2, w3, w4)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val t3 = new Task("t3")
  val t4 = new Task("t4")
  val tasks = SortedSet(t1, t2, t3, t4)

  var cost: Map[(Agent, Task), Double] = Map[(Agent,Task), Double]()
  cost+= ((w1, t1) -> 6.0)
  cost+= ((w1, t2) -> 10.0)
  cost+= ((w1, t3) -> 17.0)
  cost+= ((w1, t4) -> 24.0)
  cost+= ((w2, t1) -> 10.0)
  cost+= ((w2, t2) -> 8.0)
  cost+= ((w2, t3) -> 18.0)
  cost+= ((w2, t4) -> 22.0)
  cost+= ((w3, t1) -> 9.0)
  cost+= ((w3, t2) -> 10.0)
  cost+= ((w3, t3) -> 10.0)
  cost+= ((w3, t4) -> 24.0)
  cost+= ((w4, t1) -> 10.0)
  cost+= ((w4, t2) -> 7.0)
  cost+= ((w4, t3) -> 18.0)
  cost+= ((w4, t4) -> 14.0)
  val pb = new MATA(workers, tasks, cost)

  def main(args: Array[String]): Unit = {
    val negotiationSolver = new CentralizedSolver(pb, LCmax, SingleSwapAndSingleGift)//SingleGiftOnly
    var allocation = new Allocation(pb)
    allocation = allocation.update(w1, SortedSet(t4))
    allocation = allocation.update(w2, SortedSet(t3))
    allocation = allocation.update(w3, SortedSet(t1))
    allocation = allocation.update(w4, SortedSet(t2))
    println(allocation)
    negotiationSolver.debug = true
    println(negotiationSolver.reallocate(allocation).toString)
  }
}
