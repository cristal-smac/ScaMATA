// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{MATA, Allocation}

/**
  * Solver testing all the allocation
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class ExhaustiveSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule) {
  debug = false

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    var bestAllocation = new Allocation(pb)
      var min = Double.MaxValue
      pb.allAllocation().foreach { allocation =>
        val goal = rule match {
          case LCmax => allocation.makespan()
          case LF => allocation.flowtime()
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
  import org.scamata.example.Toy4x4._
  println(pb)
  val solver = new ExhaustiveSolver(pb,LCmax)
  println(solver.run().toString)
}