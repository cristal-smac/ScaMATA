// Copyright (C) Maxime MORGE 2018
package org.scamata.example

import org.scamata.core.{Allocation, MWSTA, Task, Worker}
import org.scamata.example.FromSituated4x4._
import org.scamata.solver.{CentralizedSolver, LCmax, LC,  SingleSwapAndSingleGift, SingleGiftOnly}

import scala.collection.SortedSet

object Situated4x4 {
  val w1 = new Worker("1")
  val w2 = new Worker("2")
  val w3 = new Worker("3")
  val w4 = new Worker("4")
  val workers = SortedSet(w1, w2, w3, w4)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val t3 = new Task("t3")
  val t4 = new Task("t4")
  val tasks = SortedSet(t1, t2, t3, t4)

  var locationMatrix : Map[(Worker, Task), Int] = Map[(Worker, Task), Int]()
  locationMatrix += ((w1,t1) -> 3)
  locationMatrix += ((w1,t2) -> 0)
  locationMatrix += ((w1,t3) -> 2)
  locationMatrix += ((w1,t4) -> 0)
  locationMatrix += ((w2,t1) -> 0)
  locationMatrix += ((w2,t2) -> 5)
  locationMatrix += ((w2,t3) -> 0)
  locationMatrix += ((w2,t4) -> 1)
  locationMatrix += ((w3,t1) -> 2)
  locationMatrix += ((w3,t2) -> 0)
  locationMatrix += ((w3,t3) -> 4)
  locationMatrix += ((w3,t4) -> 0)
  locationMatrix += ((w4,t1) -> 0)
  locationMatrix += ((w4,t2) -> 1)
  locationMatrix += ((w4,t3) -> 0)
  locationMatrix += ((w4,t4) -> 6)
  val pb  = new MWSTA(workers, tasks, locationMatrix)

  def main(args: Array[String]): Unit = {
    println(pb)
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
