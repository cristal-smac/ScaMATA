// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.util.Random
import scala.collection.SortedSet

/**
  * Class representing a a Multi-Worker Situated Task Allocation problem
  * @param workers performing the tasks
  * @param tasks to be performed
  * @param locationMatrix the number of chunks for each task for each worker
  */
class MWSTA(workers: SortedSet[Worker], tasks: SortedSet[Task], val locationMatrix : Map[(Worker, Task), Int]) extends
  MWTA(workers: SortedSet[Worker], tasks: SortedSet[Task]){

  locationMatrix.foreach{ case ((w : Worker, t: Task), nbChunks : Int) =>
    costMatrix += ((w, t) -> (nbChunks + 2 *
    locationMatrix.filterKeys( k  => k._1 != w && k._2 == t).foldLeft(0)(_+_._2) )
      )
  }
}

/**
  * Testing a  Multi-Worker Situated Task Allocation problem
  */
object MWSTA extends App{
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
  locationMatrix += ((w1,t1) -> 4)
  locationMatrix += ((w1,t2) -> 0)
  locationMatrix += ((w1,t3) -> 1)
  locationMatrix += ((w1,t4) -> 0)
  locationMatrix += ((w2,t1) -> 0)
  locationMatrix += ((w2,t2) -> 3)
  locationMatrix += ((w2,t3) -> 0)
  locationMatrix += ((w2,t4) -> 2)
  locationMatrix += ((w3,t1) -> 1)
  locationMatrix += ((w3,t2) -> 0)
  locationMatrix += ((w3,t3) -> 8)
  locationMatrix += ((w3,t4) -> 0)
  locationMatrix += ((w4,t1) -> 0)
  locationMatrix += ((w4,t2) -> 3)
  locationMatrix += ((w4,t3) -> 0)
  locationMatrix += ((w4,t4) -> 10)

  val pb  = new MWSTA(workers, tasks, locationMatrix)
  println(pb)

}