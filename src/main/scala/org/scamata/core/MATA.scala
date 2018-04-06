// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.util.Random
import scala.collection.SortedSet

/**
  * Class representing a MultiAgent Task Allocation problem
  * @param agents
  * @param tasks
  * @param cost Cost Matrix
  */
class MATA(val agents: SortedSet[Worker], val tasks: SortedSet[Task], val cost : Map[(Worker, Task), Double]) {

  /**
    * Returns a string describing the MATA problem
    */
  override def toString: String = {
    "m: " +agents.size +"\n"+
      "n: " + tasks.size +"\n"+
      "agents: " +agents.mkString(", ") +"\n"+
      "tasks: " + tasks.mkString(", ") +"\n"+
      agents.toList.map(a =>
        tasks.toList.map( t =>
          s"$a: $t ${cost(a,t)}"
          ).mkString("\n")
      ).mkString("\n")
  }

  /**
    * Returns the number of agents
    */
  def m() : Int = agents.size

  /**
    * Returns the number of tasks
    */
  def n() : Int = tasks.size

  /**
    * Returns an worker
    * @param name the worker of the worker
    */
  def getAgent(name: String) : Worker = {
    agents.find(a => a.name.equals(name)) match {
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
    * Return a string description of the MATA problem in the Opyimization Progamming language
    */
  def toOPL : String = {
    "M = "+agents.size+"; \n"+
    "N = "+tasks.size+"; \n"+
      "C = "+
      agents.toList.map(a =>
        tasks.toList.map( t =>
          cost(a,t).toString
        ).mkString("[", ", ", "]")
      ).mkString("[", ", ", "] ;\n")
  }

  /**
    * Returns true if the cost of allActors tasks for allActors agents are specified
    */
  def isFullySpecified: Boolean = cost.size == agents.size * tasks.size

}

/**
  * Factory for [[org.scamata.core.MATA]] instances
  */
object MATA{

  val debug = false

  /**
    * Returns a random MATA proble instance
    * @param m number of agents
    * @param n number of tasks
    */
  def randomProblem(m : Int, n : Int) : MATA = {
    val agents: SortedSet[Worker] = collection.immutable.SortedSet[Worker]() ++ (for (k <- 0 until m) yield new Worker(name = s"${Random.alphanumeric take 10 mkString ""}"))
    val tasks: SortedSet[Task] = collection.immutable.SortedSet[Task]() ++  (for (k <- 0 until n) yield new Task(name = s"${Random.alphanumeric take 10 mkString ""}"))
    val cost : Map[(Worker, Task), Double] = (for(i <- 0 until m; j <- 0 until n) yield (agents.toList(i),tasks.toList(j)) -> Random.nextDouble()).toMap
    new MATA(agents, tasks, cost)
  }
}