// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.deal._

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
    var activeAgents = pb.agents
    if (debug) println("All agents are initally active")
    while(activeAgents.nonEmpty){
      activeAgents.foreach { initiator: Worker =>
        if (debug) println(s"$initiator tries to find a social rational gift")
        var bestGoal = rule match {
          case Cmax => allocation.makespan()
          case Flowtime => allocation.flowtime()
        }
        var bestAllocation: Allocation = allocation
        var bestSingleGift: Gift = new Gift(initiator, initiator, Set[Task]())
        val potentialPartners: Set[Worker] = allocation.leastLoadedAgents(initiator)
        if (debug) println(s"Potential partner: $potentialPartners")
        if (potentialPartners.isEmpty) {
          activeAgents -= initiator
          if (debug) println(s"$initiator becomes inactive")
        }
        else {
          potentialPartners.foreach { opponent =>
            allocation.bundle(initiator).foreach { task =>
              // 4 - Foreach potential gift
              val gift = new SingleGift(initiator, opponent, task)
              val modifiedAllocation = allocation.apply(gift)
              val currentGoal = rule match { // Compute the new goal
              case Cmax => modifiedAllocation.makespan()
              case Flowtime => modifiedAllocation.flowtime()
            }
              if (currentGoal < bestGoal) {
                bestGoal = currentGoal
                bestAllocation = modifiedAllocation
                bestSingleGift = gift
              }
            }
          }
          // Select the best gift if any
          if (bestAllocation.equals(allocation)) {
            if (debug) println(s"$initiator becomes inactive")
            activeAgents -= initiator
          } else {
            if (debug) println(s"$bestSingleGift is performed")
            allocation = bestAllocation
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
  import org.scamata.example.toy4x4._
  //val pb = MATA.randomProblem(2, 4)
  println(pb)
  val negotiationSolver = new GiftSolver(pb,Cmax)
  println(negotiationSolver.run().toString)

}