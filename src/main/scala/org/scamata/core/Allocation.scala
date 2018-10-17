// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import org.scamata.actor.WorkerAgent
import org.scamata.deal.{Deal, SingleGift, SingleSwap}
import org.scamata.util.RandomUtils

import scala.collection.SortedSet
import scala.io.Source

/**
  * Class representing a task allocation
  *
  * @param pb MATA
  */
class Allocation(val pb: MATA) {

  var bundle: Map[Agent, SortedSet[Task]] = Map[Agent, SortedSet[Task]]()
  pb.workers.foreach(a => bundle += a -> SortedSet[Task]())

  override def toString: String = pb.workers.toList.map(w => s"$w: " + bundle(w).toList.mkString(", ")).mkString("\n")

  /**
    * Returns the workload of the worker
    */
  def workload(worker: Agent): Double = bundle(worker).foldLeft(0.0)((acc: Double, t: Task) => acc + pb.cost(worker, t))

  /**
    * Returns the workloads
    */
  def workloads(): Map[Agent, Double] = pb.workers.toSeq.map(worker => worker -> workload(worker)).toMap

  /**
    * Returns the mean cost incurred by the task allocation
    */
  def flowtime(): Double = pb.workers.foldLeft(0.0)((acc: Double, a: Agent) => acc + workload(a))/pb.m

  /**
    * Returns the completion time of the last task to perform
    */
  def makespan(): Double = {
    var max = 0.0
    pb.workers.foreach { worker =>
      max = Math.max(max, workload(worker))
    }
    max
  }

  /**
    * Return the peers which are least loaded than the initiator
    */
  def leastLoadedAgents(initiator: Agent): Set[Agent] = pb.workers.filter(w => workload(w) < workload(initiator)).toSet

  /**
    * Returns a copy
    */
  def copy(): Allocation = {
    val allocation = new Allocation(pb)
    this.bundle.foreach {
      case (a: Agent, t: Set[Task]) =>
        allocation.bundle = allocation.bundle.updated(a, t)
      case _ => throw new RuntimeException("Not able to copy bundle")
    }
    allocation
  }

  /**
    * Update an allocation with a new bundle for a worker
    */
  def update(worker: Agent, bundle: SortedSet[Task]): Allocation = {
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(worker, bundle)
    allocation
  }

  def apply(deal: Deal): Allocation = {
    deal match {
      case g: SingleGift => apply(g.asInstanceOf[SingleGift])
      case s: SingleSwap => apply(s.asInstanceOf[SingleSwap])
      case d: Deal => throw new RuntimeException(s"Do not know how to apply deal $d")
    }
  }

  /**
    * The provider gives a task to the supplier
    */
  def apply(gift: SingleGift): Allocation = {
    if (!bundle(gift.provider).contains(gift.task)) throw new RuntimeException(s"${gift.provider} cannot give ${gift.task}")
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(gift.provider, allocation.bundle(gift.provider) - gift.task)
    allocation.bundle = allocation.bundle.updated(gift.supplier, allocation.bundle(gift.supplier) + gift.task)
    allocation
  }

  /**
    * Two worker apply a single task swap
    */
  def apply(swap: SingleSwap): Allocation = {
    if (!bundle(swap.worker1).contains(swap.task1) || !bundle(swap.worker2).contains(swap.task2)) throw new RuntimeException(s"${swap} cannot be performed on $this")
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(swap.worker1, allocation.bundle(swap.worker1) - swap.task1 + swap.task2)
    allocation.bundle = allocation.bundle.updated(swap.worker2, allocation.bundle(swap.worker2) + swap.task1 - swap.task2)
    allocation
  }

  /**
    * Returns all the single swaps between two peers
    */
  def allSingleSwap(worker1: Agent, worker2: Agent): Set[SingleSwap] = {
    var swaps = Set[SingleSwap]()
    bundle(worker1).foreach { t1 =>
      bundle(worker2).foreach { t2 =>
        swaps += new SingleSwap(worker1, worker2, t1, t2)
      }
    }
    swaps
  }

  /**
    * Returns true if the allocation is sound (a complete partition of the tasks)
    */
  def isSound: Boolean =
    pb.tasks == bundle.values.foldLeft(Set[Task]())((acc, bundle) => acc ++ bundle.toSet) &&
      bundle.values.forall(b1 => bundle.values.filter(_ != b1).forall(b2 => b2.toSet.intersect(b1.toSet).isEmpty))

  /**
    * Returns true if a task is performed by at most one task
    */
  def isCoherent: Boolean = {
    pb.workers.foreach { i =>
      bundle(i).foreach { t =>
        (pb.workers - i).foreach { j =>
          if (bundle(j).contains(t)) return false
        }
      }
    }
    true
  }

  /**
    * Return true if each agent has at most one task
    */
  def isSingle: Boolean = {
    pb.workers.foreach { i =>
        if (bundle(i).size > 1) return false
    }
    true
  }

  /**
    * Returns the allocated tasks
    */
  def allocatedTasks() : Set[Task] = {
    var tasks = Set[Task]()
    pb.workers.foreach{ w =>
      tasks ++= bundle(w)
    }
    tasks
  }

  /**
    * Returns the allocated tasks
    */
  def unAllocatedTasks() : Set[Task] = {
    pb.tasks.toSet -- allocatedTasks()
  }

}

/**
  * Factory for [[Allocation]] instances
  */
object Allocation {
  val debug = false

  /**
    * Build an allocation
    *
    * @param path of the OPL output
    * @param pb   MATA
    */
  def apply(path: String, pb: MATA): Allocation = {
    val allocation = new Allocation(pb)
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
        val agent = pb.workers.toVector(agentNumber - 1)
        if (debug) println(s"${agent.name} -> ${task.name}")
        allocation.bundle += (agent -> (allocation.bundle(agent) + task))
      }
      linenumber += 1
    }
    allocation
  }


  /**
    * Generate a random allocation
    */
  def randomAllocation(pb: MATA): Allocation = {
    val allocation = new Allocation(pb)
    val r = scala.util.Random
    var availableWorkers = pb.workers
    pb.tasks.foreach { t =>
      val randomWorker = RandomUtils.random[Agent](availableWorkers)
      if (pb.n() == pb.m()) availableWorkers -= randomWorker
      var newBundle: SortedSet[Task] = allocation.bundle(randomWorker)
      newBundle += t
      allocation.bundle += (randomWorker -> newBundle)
    }
    allocation
  }
}