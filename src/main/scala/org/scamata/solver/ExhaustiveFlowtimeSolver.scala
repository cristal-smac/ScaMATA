// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}

import scala.collection.SortedSet

/**
  * Multiagent negotiation process for minimizing the rule
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class ExhaustiveFlowtimeSolver(pb: MWTA, rule: SocialRule) extends DealSolver(pb, rule) {
  debug = false
  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    var bestAllocation = new Allocation(pb)
    pb.tasks.foreach{ task =>
      val worker = pb.faster(task)
      bestAllocation = bestAllocation.update(worker, bestAllocation.bundle(worker)+task)
    }
    bestAllocation
  }
}


