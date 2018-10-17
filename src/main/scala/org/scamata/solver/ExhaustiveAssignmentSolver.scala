// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}

/**
  * Solver testing all the assignement to get best one
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class ExhaustiveAssignementSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule) {
  debug = false

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    if (pb.n() != pb.m()) new RuntimeException("ExhaustiveAssignementSolver requires a single task per worker agent")
    var min = Double.MaxValue
    var bestAllocation = new Allocation(pb)
    val tasks = pb.tasks.toList
    tasks.permutations.foreach { permutation =>
      var todo = permutation
      var allocation = new Allocation(pb)
      pb.workers.foreach { i =>
        allocation = allocation.update(i, allocation.bundle(i) + todo.head)
        todo = todo.tail
      }
      val goal = rule match {
        case LCmax => allocation.makespan()
        case LC => allocation.meanWorkload()
      }
      if (goal < min) {
        bestAllocation = allocation
        min = goal
      }
    }
    return bestAllocation
  }
}

