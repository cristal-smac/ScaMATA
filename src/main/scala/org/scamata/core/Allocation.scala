// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.io.Source

/**
  * Class representing a task allocation
  * @param pb MATA
  */
class Allocation(val pb : MATA){

  var bundle : Map[Agent, Set[Task]] = Map[Agent, Set[Task]]()
  pb.agents.foreach(a => bundle += a -> Set[Task]())

  override def toString: String = pb.agents.toList.map( a => s"$a: "+bundle(a).toList.mkString(", ") ).mkString("\n")

  /**
    * Returns the workload of the agent
    */
  def workload(agent: Agent) : Double = bundle(agent).foldLeft(0.0)((acc : Double, t : Task) => acc + pb.cost(agent, t))

  /**
    * Returns he total costs incurred by the task allocation
    */
  def flowtime() : Double = pb.agents.foldLeft(0.0)((acc : Double, a : Agent) => acc + workload(a))

  /**
    * Returns the completion time of the last task to perform
    */
  def makespan() : Double = pb.agents.foldLeft(0.0)( (max, a) => math.max(max, workload(a)))

}

/**
  * Factory for [[Allocation]] instances
  */
object Allocation{
  val debug = false

  /**
    * Build an allocation
    * @param path of the OPL output
    * @param pb MATA
    */
  def apply(path: String, pb : MATA): Allocation = {
    var allocation = new Allocation(pb)
    val bufferedSource = Source.fromFile(path)
    var linenumber = 0
    for (line <- bufferedSource.getLines) { // foreach line
      if (linenumber == 0) {
        val u = line.toDouble
        if (debug) println(s"Rule = $u")
      }
      if (linenumber == 1) {
        val t = line.toDouble
        if (debug) println(s"T (ms) = $t")
      }
      if (linenumber > 1) {
        val agent : Agent = pb.agents.toVector(linenumber - 2)
        val taskNumber = line.toInt
        if (taskNumber == 0) {
          if (debug) println(s"${agent.name} -> empty")
          allocation.bundle+= (agent -> Set[Task]())
        }
        else {
          val task = pb.tasks.toVector(taskNumber - 1)
          if (debug) println(s"${agent.name} -> ${task.name}")
          allocation.bundle+= (agent -> (allocation.bundle(agent) + task))
        }
      }
      linenumber += 1
    }
    allocation
  }
}