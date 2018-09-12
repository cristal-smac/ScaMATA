// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.util.Random
import scala.collection.SortedSet

/**
  * Class representing a Multi-Agent Task Allocation problem
  * @param workers performing the tasks
  * @param tasks to be performed
  */
class MATA(val workers: SortedSet[Agent], val tasks: SortedSet[Task]) {

  var costMatrix : Map[(Agent, Task), Double] = Map[(Agent, Task), Double]()

  /**
    * Constructor
    * @param workers performing the tasks
    * @param tasks to be performed
    * @param costMatrix cost matrix for performing the task by the worker
    */
  def this(workers: SortedSet[Agent], tasks: SortedSet[Task], costMatrix : Map[(Agent, Task), Double]) ={
    this(workers, tasks)
    this.costMatrix = costMatrix
  }

  /**
    * Return the cost of a task for a worker, eventually 0.0 if NoTask
     */
  def cost(worker: Agent, task: Task): Double = if (task != NoTask) costMatrix(worker, task) else 0.0

  /**
    * Returns a string describing the MATA problem
    */
  override def toString: String = {
    "m: " + workers.size + "\n" +
      "n: " + tasks.size + "\n" +
      "peers: " + workers.mkString(", ") + "\n" +
      "tasks: " + tasks.mkString(", ") + "\n" +
      workers.toList.map(a =>
        tasks.toList.map(t =>
          s"$a: $t ${cost(a, t)}"
        ).mkString("\n")
      ).mkString("\n")
  }

  /**
    * Returns the number of peers
    */
  def m(): Int = workers.size

  /**
    * Returns the number of tasks
    */
  def n(): Int = tasks.size

  /**
    * Returns the faster worker for a task
    */
  def faster(task : Task) : Agent = {
    var best : Agent = NoAgent
    var minCost = Double.MaxValue
    workers.foreach{ worker =>
      val c = cost(worker, task)
      if (c < minCost) {
        best = worker
        minCost = c
      }
    }
    best
  }

  /**
    * Returns an worker
    *
    * @param name of the worker
    */
  def getWorker(name: String): Agent = {
    workers.find(a => a.name.equals(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException("No worker " + name + " has been found")
    }
  }

  /**
    * Returns a task
    *
    * @param name the worker of the task
    */
  def getTask(name: String): Task = {
    tasks.find(t => t.name.equals(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException("No task " + name + " has been found")
    }
  }

  /**
    * Return a string description of the MATA problem in the Optimization Programming Language
    */
  def toOPL: String = {
    "M = " + workers.size + "; \n" +
      "N = " + tasks.size + "; \n" +
      "C = " +
      workers.toList.map(a =>
        tasks.toList.map(t =>
          cost(a, t).toString
        ).mkString("[", ", ", "]")
      ).mkString("[", ", ", "] ;\n")
  }

  /**
    * Returns true if the cost of allActors tasks for allActors peers are specified
    */
  def isFullySpecified: Boolean = costMatrix.size == workers.size * tasks.size

  /**
    * Returns all the potential allocations
    */
  def allAllocation(): Set[Allocation] = allAllocation(workers, tasks)

  /**
    * Returns all the potential allocations
    *
    * @param workers
    * @param tasks
    */
  def allAllocation(workers: SortedSet[Agent], tasks: SortedSet[Task]): Set[Allocation] = {
    if (workers.size == 1) {
      var allocation = new Allocation(this)
      allocation = allocation.update(workers.head, tasks)
      return Set(allocation) // returns allocation where workers has no task
    }
    var allocations = Set[Allocation]()
    val worker = workers.head // Select one worker
    val otherWorkers = workers - worker
    tasks.subsets().foreach { bundle => // For each subset of tasks
      var complementary = tasks -- bundle
      val subAllocations = allAllocation(otherWorkers, complementary) // compute the sub-allocation of the complementary
      // and allocate the current bundle to the worker
      subAllocations.foreach { a =>
        val newAllocation = a.update(worker, bundle)
        allocations += newAllocation
      }
    }
    allocations
  }
}

/**
  * Factory for [[org.scamata.core.MATA]] instances
  */
object MATA{

  val debug = false

  val MAXCOST = 1000
  /**
    * Returns a random MATA problem instance
    * @param m number of peers
    * @param n number of tasks
    */
  def randomProblem(m : Int, n : Int) : MATA = {
    val workers: SortedSet[Agent] = collection.immutable.SortedSet[Agent]() ++ (for (k <- 1 until m+1) yield new Agent(name = s"w$k"))
    val tasks: SortedSet[Task] = collection.immutable.SortedSet[Task]() ++  (for (k <- 1 until n+1) yield new Task(name = s"t$k"))
    val cost : Map[(Agent, Task), Double] = (for(i <- 0 until m; j <- 0 until n) yield (workers.toList(i),tasks.toList(j)) -> (Random.nextInt(MAXCOST)).toInt.toDouble ).toMap
    new MATA(workers, tasks, cost)
  }

  /**
    * Test random problem generation
    */
  def main(args: Array[String]): Unit = {
    val pb = MATA.randomProblem(10, 100)
    println(pb)
    val allocation =Allocation.randomAllocation(pb)
    println(allocation)
    println(allocation.makespan())
  }
}

