// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{MATA, Allocation}
import org.scamata.deal._

import scala.collection.SortedSet

/**
  * Multiagent negotiation process for minimizing the rule
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class ExhaustiveSolver(pb: MATA, rule: SocialRule) extends DealSolver(pb, rule) {
  debug = false

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    var min = Double.MaxValue
    var bestAllocation = new Allocation(pb)
    pb.allAllocation().foreach { allocation =>
      val goal = rule match {
        case Cmax =>
          allocation.makespan()
        case Flowtime =>
          allocation.flowtime()
      }
      if (goal < min) {
        bestAllocation = allocation
        min = goal
      }

    }
    bestAllocation
  }
}


/**
  * Companion object to test it
  */
object ExhaustiveSolver extends App {
  val debug = false
  //import org.scamata.example.toy2x4._
  val pb = MATA.randomProblem(2, 20)
  println(pb)
  val solver = new ExhaustiveSolver(pb,Cmax)
  println(solver.run().toString)

}