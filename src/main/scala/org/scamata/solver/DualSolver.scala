// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.MATA

/**
  * Abstract class for solving an MATA problem by translating the problem/outcome
  */
abstract class DualSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule){

  var preSolvingTime : Long = 0

  var postSolvingTime : Long = 0

}
