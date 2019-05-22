// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import org.scamata.util.{MathUtils, RandomUtils}

import scala.util.Random
import scala.collection.SortedSet

/**
  * Class representing a Multi-Agent Task Allocation problem
  * @param workers performing the tasks
  * @param tasks to be performed
  */
class MATA(val workers: SortedSet[Worker], val tasks: SortedSet[Task]) {

  var costMatrix : Map[(Worker, Task), Double] = Map[(Worker, Task), Double]()

  /**
    * Constructor
    * @param workers performing the tasks
    * @param tasks to be performed
    * @param costMatrix cost matrix for performing the task by the worker
    */
  def this(workers: SortedSet[Worker], tasks: SortedSet[Task], costMatrix : Map[(Worker, Task), Double]) ={
    this(workers, tasks)
    this.costMatrix = costMatrix
  }

  /**
    * Return the cost of a task for a worker, eventually 0.0 if NoTask
     */
  def cost(worker: Worker, task: Task): Double = if (task != NoTask) costMatrix(worker, task) else 0.0

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
  def faster(task : Task) : Worker = {
    var best : Worker = NoWorker$
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
  def getWorker(name: String): Worker = {
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
      * Return a string description of the transportation problem in the Optimization Programming Language
      */
    def toTOPL: String = {
      var s= Seq[java.io.Serializable]()
      for (k <- 1 to tasks.size) {
        s :+= workers.toList.map(a =>
          tasks.toList.map(t =>
            (k*cost(a, t)).toString
          ).mkString("[", ", ", "]"))
      }
      "M = " + workers.size + "; \n" +
        "N = " + tasks.size + "; \n" +
        "Q = " + s.mkString("[", ", ", "] ;\n").replace("List(", "").replace(")", "")
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
  def allAllocation(workers: SortedSet[Worker], tasks: SortedSet[Task]): Set[Allocation] = {
    if (workers.size == 1) {
      var allocation = new Allocation(this)
      allocation = allocation.update(workers.head, tasks)
      return Set(allocation) // returns allocation where workers has no task
    }
    var allocations = Set[Allocation]()
    val worker = workers.head // Select one worker
    val otherWorkers = workers - worker
    tasks.subsets().foreach { bundle => // For each subset of tasks
      val complementary = tasks -- bundle
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
  * Class representing a social rule
  */
class RandomGenerationRule
case object Uncorrelated extends RandomGenerationRule
case object MachineCorrelated extends RandomGenerationRule
case object TaskCorrelated extends RandomGenerationRule
case object MachineTaskCorrelated extends RandomGenerationRule

/**
  * Factory for [[org.scamata.core.MATA]] instances
  */
object MATA{

  val debug = false

  val MAXCOST = 100
  def uniformCost() : Double = (Random.nextInt(MAXCOST)+1).toDouble

  /**
    * Returns a random MATA problem instance
    * @param m number of peers
    * @param n number of tasks
    */
  def randomProblem(m : Int, n : Int, rule : RandomGenerationRule) : MATA = {
    val workers: SortedSet[Worker] = collection.immutable.SortedSet[Worker]() ++
      (for (k <- 1 until m+1) yield new Worker(name = s"w$k"))
    val tasks: SortedSet[Task] = collection.immutable.SortedSet[Task]() ++
      (for (k <- 1 until n+1) yield new Task(name = s"t$k"))
    val beta : Array[Double] =  Array.fill(n)(
      if (rule == MachineTaskCorrelated) RandomUtils.random(1,20)
      else RandomUtils.random(1,MAXCOST)
    )
    val alpha : Array[Double] =  Array.fill(m)(
      if (rule == MachineTaskCorrelated) RandomUtils.random(1,20)
      else RandomUtils.random(1,MAXCOST)
    )
    var cost = Map[(Worker, Task), Double]()
    for(i <- 0 until m){
      for (j <- 0 until n){
        val value : Double  = rule match {
          case Uncorrelated => RandomUtils.random(1,MAXCOST)
          case MachineCorrelated => alpha(i)+ RandomUtils.random(1,20)
          case TaskCorrelated => beta(j) + RandomUtils.random(1,20)
          case MachineTaskCorrelated => alpha(i)* beta(j) + RandomUtils.random(1,20)
        }
        cost = cost + ( (workers.toList(i),tasks.toList(j)) -> value )
      }
    }
    new MATA(workers, tasks, cost)
  }

  /**
    * Test random problem generation
    */
  def main(args: Array[String]): Unit = {
    val pb = MATA.randomProblem(10, 100, Uncorrelated)
    println(pb)
    val allocation =Allocation.randomAllocation(pb)
    println(allocation)
    println(allocation.makespan())
  }
}

