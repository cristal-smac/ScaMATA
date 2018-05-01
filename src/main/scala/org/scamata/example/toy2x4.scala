package org.scamata.example

import org.scamata.core.{Worker, MATA, Task}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */
object toy2x4{
  val a1 = new Worker("a1")
  val a2 = new Worker("a2")
  val agents = SortedSet(a1, a2)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val t3 = new Task("t3")
  val t4 = new Task("t4")
  val tasks = SortedSet(t1, t2, t3, t4)

  var cost: Map[(Worker, Task), Double] = Map[(Worker,Task), Double]()
  cost+= ((a1, t1) -> 993.0)
  cost+= ((a1, t2) -> 967.0)
  cost+= ((a1, t3) -> 529.0)
  cost+= ((a1, t4) -> 623.0)
  cost+= ((a2, t1) -> 205.0)
  cost+= ((a2, t2) -> 390.0)
  cost+= ((a2, t3) -> 6.0)
  cost+= ((a2, t4) -> 604.0)
  val pb = new MATA(agents, tasks, cost)
}
