// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import org.scamata.deal.{SingleGift, SingleSwap}
import org.scamata.util.RandomUtils

import scala.collection.SortedSet
import scala.io.Source

/**
  * Class representing a task allocation
  * @param pb MATA
  */
class Allocation(val pb: MATA) {

  var bundle: Map[Worker, SortedSet[Task]] = Map[Worker, SortedSet[Task]]()
  pb.workers.foreach(a => bundle += a -> SortedSet[Task]())

  override def toString: String = pb.workers.toList.map(a => s"$a: " + bundle(a).toList.mkString(", ")).mkString("\n")

  /**
    * Returns the workload of the worker
    */
  def workload(worker: Worker): Double = bundle(worker).foldLeft(0.0)((acc: Double, t: Task) => acc + pb.cost(worker, t))

  /**
    * Returns the workloads
    */
  def workloads(): Map[Worker, Double] = pb.workers.toSeq.map( worker => worker -> workload(worker) ).toMap

  /**
    * Returns the total costs incurred by the task allocation
    */
  def flowtime(): Double = pb.workers.foldLeft(0.0)((acc: Double, a: Worker) => acc + workload(a))

  /**
    * Returns the completion time of the last task to perform
    */
  def makespan(): Double = {
    var max = 0.0
    pb.workers.foreach{ worker =>
        max = Math.max(max, workload(worker))
    }
    max
    //foldLeft(0.0)((max, a) => math.max(max, workload(a)))
  }

  /**
    * Return the peers which are least loaded than the initiator
    */
  def leastLoadedAgents(initiator: Worker): Set[Worker] = pb.workers.filter(a => workload(a) < workload(initiator)).toSet

  /**
    * Returns a copy
    */
  def copy(): Allocation = {
    val allocation = new Allocation(pb)
    this.bundle.foreach { case (a: Worker, t: Set[Task]) =>
      allocation.bundle = allocation.bundle.updated(a, t)
    }
    allocation
  }

  /**
    * Update an allocation for a worker with a new bundle
    */
  def update(worker: Worker, bundle : SortedSet[Task]) : Allocation = {
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(worker, bundle)
    allocation
  }

  /**
    * The provider gives a task to the supplier
    */
  def gift(gift: SingleGift): Allocation = {
    if (!bundle(gift.provider).contains(gift.task)) throw new RuntimeException(s"${gift.provider} cannot give ${gift.task}")
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(gift.provider, allocation.bundle(gift.provider) - gift.task)
    allocation.bundle = allocation.bundle.updated(gift.supplier, allocation.bundle(gift.supplier) + gift.task)
    allocation
  }

  /**
    * Two worker swap a single task
    */
  def swap(swap: SingleSwap): Allocation = {
    if (!bundle(swap.worker1).contains(swap.task1) || !bundle(swap.worker2).contains(swap.task2)) throw new RuntimeException(s"${swap} cannot be performed on $this")
    val allocation = this.copy()
    allocation.bundle = allocation.bundle.updated(swap.worker1, allocation.bundle(swap.worker1) - swap.task1 + swap.task2)
    allocation.bundle = allocation.bundle.updated(swap.worker2, allocation.bundle(swap.worker2) + swap.task1 - swap.task2)
    allocation
  }

  /**
    * Returns all the single swaps between two peers
    */
  def allSingleSwap(worker1: Worker, worker2: Worker): Set[SingleSwap] = {
    var swaps = Set[SingleSwap]()
    bundle(worker1).foreach { t1 =>
      bundle(worker2).foreach { t2 =>
        swaps += new SingleSwap(worker1, worker2, t1, t2)
      }
    }
    swaps
  }


  def isSound: Boolean =
    pb.tasks == bundle.values.foldLeft(Set[Task]())((acc, bundle) => acc ++ bundle.toSet) &&
    bundle.values.forall( b1 =>  bundle.values.filter(_ != b1).forall( b2 => b2.toSet.intersect(b1.toSet).isEmpty))

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
      pb.tasks.foreach { t =>
        val randomWorker = RandomUtils.random[Worker](pb.workers)
        var newBundle: SortedSet[Task] = allocation.bundle(randomWorker)
        newBundle += t
        allocation.bundle += (randomWorker -> newBundle)
      }
      allocation
    }
  }