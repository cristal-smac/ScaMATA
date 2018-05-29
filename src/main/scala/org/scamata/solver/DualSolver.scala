// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.MWTA

/**
  * Abstract class for solving an MWTA problem by translating the problem/outcome
  */
abstract class DualSolver(pb: MWTA, rule: SocialRule) extends Solver(pb, rule){
  var preSolvingTime : Long = 0
  var postSolvingTime : Long = 0
}
