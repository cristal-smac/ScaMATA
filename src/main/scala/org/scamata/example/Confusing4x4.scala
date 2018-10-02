package org.scamata.example

import org.scamata.core.{Agent, MATA, Task}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */

object Confusing4x4{
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
  cost+= ((w1, t1) -> 324.0)
  cost+= ((w1, t2) -> 557.0)
  cost+= ((w1, t3) -> 590.0)
  cost+= ((w1, t4) -> 293.0)
  cost+= ((w2, t1) -> 533.0)
  cost+= ((w2, t2) -> 731.0)
  cost+= ((w2, t3) -> 93.0)
  cost+= ((w2, t4) -> 910.0)
  cost+= ((w3, t1) -> 374.0)
  cost+= ((w3, t2) -> 855.0)
  cost+= ((w3, t3) -> 124.0)
  cost+= ((w3, t4) -> 984.0)
  cost+= ((w4, t1) -> 424.0)
  cost+= ((w4, t2) -> 692.0)
  cost+= ((w4, t3) -> 147.0)
  cost+= ((w4, t4) -> 366.0)
  val pb = new MATA(workers, tasks, cost)
}
