// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.collection.SortedSet
import org.scamata.solver._


/**
  * Class representing a MultiAgent Task Allocation problem
  * @param agents
  * @param tasks
  * @param cost Cost Matrix
  */
class MATA(val agents: SortedSet[Agent], val tasks: SortedSet[Task], val cost : Map[(Agent, Task), Double]) {

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
    * Returns an agent
    * @param name the name of the agent
    */
  def getAgent(name: String) : Agent = {
    agents.find(a => a.name.equals(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException("No agent "+name+" has been found")
    }
  }

  /**
    * Returns a task
    * @param name the name of the task
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
    * Returns true if the cost of all tasks for all agents are specified
    */
  def isFullySpecified: Boolean = cost.size == agents.size * tasks.size

}

/**
  * Companion object to test it
  */
object MATA extends App {
  val debug = true
  import org.scamata.example.toy4x4._
  println(pb)
  val solver = new LPSolver(pb,Cmax)
  printf(solver.solve().toString)
}