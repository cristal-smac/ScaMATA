// Copyright (C) Maxime MORGE 2018
package org.scamata.example

import org.scamata.core.{MATA, Task, Worker}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */

object Toy4x2{
  val w1 = new Worker("1")
  val w2 = new Worker("2")
  val w3 = new Worker("3")
  val w4 = new Worker("4")
  val workers = SortedSet(w1, w2, w3, w4)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val tasks = SortedSet(t1, t2)

  var cost: Map[(Worker, Task), Double] = Map[(Worker,Task), Double]()
  cost+= ((w1, t1) -> 6.0)
  cost+= ((w1, t2) -> 12.0)
  cost+= ((w2, t1) -> 10.0)
  cost+= ((w2, t2) -> 9.0)
  cost+= ((w3, t1) -> 4.0)
  cost+= ((w3, t2) -> 3.0)
  cost+= ((w4, t1) -> 3.0)
  cost+= ((w4, t2) -> 4.0)
  val pb = new MATA(workers, tasks, cost)
}
