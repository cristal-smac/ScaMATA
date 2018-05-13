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
class SwapSolver(pb : MWTA, rule : SocialRule) extends Solver(pb, rule) {

  /**
    * Returns a random allocation with no more improving swap
    */
  override protected def solve(): Allocation = {
    var allocation = Allocation.randomAllocation(pb)
    var activeWorkers = pb.workers
    while(activeWorkers.nonEmpty){
      activeWorkers.foreach{ initiator =>
        val workload = allocation.workload(initiator)
        val potentialPartner = rule match {
          case Cmax => pb.workers.filterNot(_ == initiator).filter( allocation.workload(_) < workload )
          case Flowtime => pb.workers.filterNot(_ == initiator)
        }
        if (potentialPartner.isEmpty || allocation.bundle(initiator).isEmpty){
          activeWorkers -= initiator
        }else{
          var bestGoal = rule match {
            case Cmax => allocation.makespan()
            case Flowtime => allocation.flowtime()
          }
          var found = false
          var bestSwap =  new SingleSwap(initiator, initiator,  allocation.bundle(initiator).head, allocation.bundle(initiator).head)
          potentialPartner.foreach{ opponent =>
            allocation.allSingleSwap(initiator, opponent).foreach{ swap : SingleSwap =>
              val potentialAllocation = allocation.swap(swap)
              val goal = rule match {
                case Cmax => potentialAllocation.makespan()
                case Flowtime => potentialAllocation.flowtime()
              }
              if (goal < bestGoal){
                found = true
                bestSwap = swap
                bestGoal = goal
              }
            }
          }
          if (!found) {
            activeWorkers -= initiator
          }else{
            allocation = allocation.swap(bestSwap)
            activeWorkers += bestSwap.worker2
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
object SwapSolver extends App {
  val debug = false
  //import org.scamata.example.toy2x4._
  val pb = MWTA.randomProblem(10, 100)
  println(pb)
  val negotiationSolver = new SwapSolver(pb,Flowtime)
  println(negotiationSolver.run().toString)

}