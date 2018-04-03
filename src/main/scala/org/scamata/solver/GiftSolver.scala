// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.deal._

/**
  * Multiagent  negotiation process for minimizing the
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
    if (debug) println(s"Start with a random allocation:\n$allocation")
    var activeAgents = pb.agents
    if (debug) println("All agents are initally active")
    while(activeAgents.nonEmpty){
      activeAgents.foreach { initiator: Agent =>
        if (debug) println(s"$initiator tries to find a social rational gift")
        var bestGoal = rule match {
          case Cmax => allocation.makespan()
          case Flowtime => allocation.flowtime()
        }
        var bestAllocation: Allocation = allocation
        var bestSingleGift: Gift = new Gift(initiator, initiator, Set[Task]())
        val potentialPartner: Set[Agent] = allocation.leastLoadedAgents(initiator)
        if (debug) println(s"Potential partner: $potentialPartner")
        if (potentialPartner.isEmpty) {
          activeAgents -= initiator
          if (debug) println(s"$initiator becomes inactive")
        }
        else {
          potentialPartner.foreach { opponent =>
            allocation.bundle(initiator).foreach { task =>
              // 4 - Foreach potential gift
              val gift = new SingleGift(initiator, opponent, task)
              val modifiedAllocation = allocation.apply(gift)
              val currentGoal = rule match { // Compute the new goal
                case Cmax => allocation.makespan()
                case Flowtime => allocation.flowtime()
              }
              if (currentGoal < bestGoal) {
                bestGoal = currentGoal
                bestAllocation = modifiedAllocation
                bestSingleGift = gift
              }
            }
            // Select the best gift if anny
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
    }
    allocation
  }
}