// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}

/**
  * Multiagent negotiation process for minimizing the rule
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class RandomSolver(pb: MATA, rule: SocialRule) extends DealSolver(pb, rule) {
  debug = false

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = Allocation.randomAllocation(pb)

}


