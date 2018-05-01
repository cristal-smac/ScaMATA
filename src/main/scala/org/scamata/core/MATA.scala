// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.util.Random
import scala.collection.SortedSet

/**
  * Class representing a MultiAgent Task Allocation problem
  * @param workers
  * @param tasks
  * @param cost Matrix
  */
class MATA(val workers: SortedSet[Worker], val tasks: SortedSet[Task], val cost : Map[(Worker, Task), Double]) {

  /**
    * Returns a string describing the MATA problem
    */
  override def toString: String = {
    "m: " +workers.size +"\n"+
      "n: " + tasks.size +"\n"+
      "workers: " +workers.mkString(", ") +"\n"+
      "tasks: " + tasks.mkString(", ") +"\n"+
      workers.toList.map(a =>
        tasks.toList.map( t =>
          s"$a: $t ${cost(a,t)}"
          ).mkString("\n")
      ).mkString("\n")
  }

  /**
    * Returns the number of workers
    */
  def m() : Int = workers.size

  /**
    * Returns the number of tasks
    */
  def n() : Int = tasks.size

  /**
    * Returns an worker
    * @param name of the worker
    */
  def getAgent(name: String) : Worker = {
    workers.find(a => a.name.equals(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException("No worker "+name+" has been found")
    }
  }

  /**
    * Returns a task
    * @param name the worker of the task
    */
  def getTask(name: String) : Task= {
    tasks.find(t => t.name.equals(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException("No task "+name+" has been found")
    }
  }

  /**
    * Return a string description of the MATA problem in the Optimization Programming language
    */
  def toOPL : String = {
    "M = "+workers.size+"; \n"+
    "N = "+tasks.size+"; \n"+
      "C = "+
      workers.toList.map(a =>
        tasks.toList.map( t =>
          cost(a,t).toString
        ).mkString("[", ", ", "]")
      ).mkString("[", ", ", "] ;\n")
  }

  /**
    * Returns true if the cost of allActors tasks for allActors workers are specified
    */
  def isFullySpecified: Boolean = cost.size == workers.size * tasks.size
}

/**
  * Factory for [[org.scamata.core.MATA]] instances
  */
object MATA{

  val debug = false

  val MAXCOST = 1000
  /**
    * Returns a random MATA proble instance
    * @param m number of workers
    * @param n number of tasks
    */
  def randomProblem(m : Int, n : Int) : MATA = {
    val workers: SortedSet[Worker] = collection.immutable.SortedSet[Worker]() ++ (for (k <- 1 until m+1) yield new Worker(name = s"w$k"))
    val tasks: SortedSet[Task] = collection.immutable.SortedSet[Task]() ++  (for (k <- 1 until n+1) yield new Task(name = s"t$k"))
    val cost : Map[(Worker, Task), Double] = (for(i <- 0 until m; j <- 0 until n) yield (workers.toList(i),tasks.toList(j)) -> (Random.nextDouble()*MAXCOST).toInt.toDouble ).toMap
    new MATA(workers, tasks, cost)
  }
}