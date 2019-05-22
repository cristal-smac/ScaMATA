// Copyright (C) Maxime MORGE 2018
package org.scamata.example

import org.scamata.core.{Worker, MATA, Task}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */

object AAMAS4x4{
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

  var cost: Map[(Worker, Task), Double] = Map[(Worker,Task), Double]()
  cost+= ((w1, t1) -> 1.0)
  cost+= ((w1, t2) -> 8.0)
  cost+= ((w1, t3) -> 10.0)
  cost+= ((w1, t4) -> 14.0)
  cost+= ((w2, t1) -> 2.0)
  cost+= ((w2, t2) -> 4.0)
  cost+= ((w2, t3) -> 12.0)
  cost+= ((w2, t4) -> 13.0)
  cost+= ((w3, t1) -> 2.0)
  cost+= ((w3, t2) -> 8.0)
  cost+= ((w3, t3) -> 8.0)
  cost+= ((w3, t4) -> 14.0)
  cost+= ((w4, t1) -> 2.0)
  cost+= ((w4, t2) -> 8.0)
  cost+= ((w4, t3) -> 12.0)
  cost+= ((w4, t4) -> 8.0)
  val pb = new MATA(workers, tasks, cost)
}
