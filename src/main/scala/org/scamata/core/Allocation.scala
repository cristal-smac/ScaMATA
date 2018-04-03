// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import org.scamata.deal.SingleGift
import org.scamata.util.RandomUtils

import scala.io.Source

/**
  * Class representing a task allocation
  * @param pb MATA
  */
class Allocation(val pb : MATA) {

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


  /**
    * Return the agents which are least loaded than the initiator
    */
  def leastLoadedAgents(initiator : Agent) : Set[Agent] = pb.agents.filter(a => workload(a) < workload(initiator)).toSet

  /**
    * Returns a copy
    */
    def copy(): Allocation = {
    val allocation = new Allocation(pb)
    bundle.foreach{ case (a: Agent, t: Set[Task]) =>
      allocation.bundle+= (a -> t)
    }
    allocation
  }

  /**
    * The provider gives a task to the supplier
    */
  def apply(gift: SingleGift) : Allocation = {
    if (! bundle(gift.provider).contains(gift.task)) throw new RuntimeException(s"${gift.provider} cannot give ${gift.task}")
    val allocation = this.copy()
    allocation.bundle += (gift.provider -> (allocation.bundle(gift.provider) - gift.task ))
    allocation.bundle += (gift.supplier -> (allocation.bundle(gift.supplier) + gift.task ))
    allocation
  }

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
        val task: Task = pb.tasks.toVector(linenumber - 2)
        val agentNumber = line.toInt
        val agent = pb.agents.toVector(agentNumber - 1)
          if (debug) println(s"${agent.name} -> ${task.name}")
          allocation.bundle+= (agent -> (allocation.bundle(agent) + task))
      }
      linenumber += 1
    }
    allocation
  }


  /**
    * Generate a random allocation
    */
  def randomAllocation(pb: MATA) : Allocation = {
    val allocation = new Allocation(pb)
    val r = scala.util.Random
    pb.tasks.foreach { t =>
      val ra =RandomUtils.random[Agent](pb.agents)
      var b : Set[Task] = allocation.bundle(ra)
      b+=t
      allocation.bundle += (ra -> b)
    }
    allocation
  }
}