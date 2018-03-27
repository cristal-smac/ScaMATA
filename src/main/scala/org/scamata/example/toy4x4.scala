package org.scamata.example

import org.scamata.core.{Agent, MATA, Task}

import scala.collection.SortedSet

/**
  * A toy MATA problem example
  */
object toy4x4{
  val a1 = new Agent("a1")
  val a2 = new Agent("a2")
  val a3 = new Agent("a3")
  val a4 = new Agent("a4")
  val agents = SortedSet(a1, a2, a3, a4)

  val t1 = new Task("t1")
  val t2 = new Task("t2")
  val t3 = new Task("t3")
  val t4 = new Task("t4")
  val tasks = SortedSet(t1, t2, t3, t4)

  var cost: Map[(Agent, Task), Double] = Map[(Agent,Task), Double]()
  cost+= ((a1, t1) -> 2.0)
  cost+= ((a1, t2) -> 3.0)
  cost+= ((a1, t3) -> 2.0)
  cost+= ((a1, t4) -> 1.0)
  cost+= ((a2, t1) -> 3.0)
  cost+= ((a2, t2) -> 2.0)
  cost+= ((a2, t3) -> 1.0)
  cost+= ((a2, t4) -> 2.0)
  cost+= ((a3, t1) -> 4.0)
  cost+= ((a3, t2) -> 3.0)
  cost+= ((a3, t3) -> 2.0)
  cost+= ((a3, t4) -> 3.0)
  cost+= ((a4, t1) -> 3.0)
  cost+= ((a4, t2) -> 4.0)
  cost+= ((a4, t3) -> 3.0)
  cost+= ((a4, t4) -> 2.0)
  val pb = new MATA(agents, tasks, cost)


}
