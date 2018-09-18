package org.scamata.example

import org.scamata.core.{Agent, MATA, Task}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */

object Toy3x3 {
  val w1 = new Agent("1")
  val w2 = new Agent("2")
  val w3 = new Agent("3")
  val workers = SortedSet(w1, w2, w3)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val t3 = new Task("t3")
  val tasks = SortedSet(t1, t2, t3)

  var cost: Map[(Agent, Task), Double] = Map[(Agent,Task), Double]()
  cost+= ((w1, t1) -> 1.0)
  cost+= ((w1, t2) -> 2.0)
  cost+= ((w1, t3) -> 3.0)
  cost+= ((w2, t1) -> 2.0)
  cost+= ((w2, t2) -> 4.0)
  cost+= ((w2, t3) -> 6.0)
  cost+= ((w3, t1) -> 3.0)
  cost+= ((w3, t2) -> 6.0)
  cost+= ((w3, t3) -> 9.0)
  val pb = new MATA(workers, tasks, cost)
}
