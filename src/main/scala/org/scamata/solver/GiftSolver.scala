// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.deal._

import scala.collection.SortedSet

/**
  * Multiagent negotiation process for minimizing the rule
  * @param pb to be solver
  * @param rule to be optimized
  */
class GiftSolver(pb : MATA, rule : SocialRule) extends Solver(pb, rule) {
  debug = false

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    var allocation = Allocation.randomAllocation(pb)
    if (debug) println(s"Give with a random allocation:\n$allocation")
    var activeWorkers = pb.workers
    if (debug) println("All workers are initially active")
    while(activeWorkers.nonEmpty){
      activeWorkers.foreach { initiator: Worker =>
        if (debug) println(s"$initiator tries to find a social rational gift")
        val potentialPartners : SortedSet[Worker] = rule match {
          case Flowtime => // all the workers
            pb.workers.filterNot(_ == initiator)
          case Cmax => // the workers with a smallest workload
            pb.workers.filterNot(_ == initiator).filter(allocation.workload(_) < allocation.workload(initiator))
        }
        if (debug) println(s"Potential partner: $potentialPartners")
        if (potentialPartners.isEmpty || allocation.bundle(initiator).isEmpty) {
          activeWorkers -= initiator
          if (debug) println(s"$initiator becomes inactive")
        }
        else {
          var found = false
          var bestAllocation: Allocation = allocation
          var bestSingleGift: Gift = new Gift(initiator, initiator, Set[Task]())
          var bestGoal = rule match {
            case Cmax => allocation.makespan()
            case Flowtime => allocation.flowtime()
          }
          potentialPartners.foreach { opponent =>
            allocation.bundle(initiator).foreach { task =>
              // 4 - Foreach potential swap
              val gift = new SingleGift(initiator, opponent, task)
              val modifiedAllocation = allocation.gift(gift)
              val currentGoal = rule match { // Compute the new goal
              case Cmax => modifiedAllocation.makespan()
              case Flowtime => modifiedAllocation.flowtime()
            }
              if (currentGoal < bestGoal) {
                bestGoal = currentGoal
                bestAllocation = modifiedAllocation
                bestSingleGift = gift
                found = true
              }
            }
          }
          // Select the best swap if any
          if (!found) {
            if (debug) println(s"$initiator becomes inactive")
            activeWorkers -= initiator
          } else {
            if (debug) println(s"$bestSingleGift is performed")
            allocation = bestAllocation
            activeWorkers += bestSingleGift.supplier
          }
        }
      }
    }
    allocation
  }
}

/**
  * Companion object to test it
  */
object GiftSolver extends App {
  val debug = false
  //import org.scamata.example.toy4x4._
  val pb = MATA.randomProblem(10, 100)
  println(pb)
  val negotiationSolver = new GiftSolver(pb,Cmax)
  println(negotiationSolver.run().toString)

}