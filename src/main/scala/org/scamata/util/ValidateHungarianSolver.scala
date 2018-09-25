// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import org.scamata.core.{Allocation, MATA}
import org.scamata.solver.{ExhaustiveAssignementSolver, ExhaustiveSolver, HungarianSolver, LC}

/**
  * Companion object to test it
  */
object ValidateHungarianSolver extends App {
  val debug = true
  var nbInstances = 1
  while(true) {
    println(s"Instance $nbInstances")
    val pb = MATA.randomProblem(4, 4)
    if (debug) println(pb)
    val exSolver = new ExhaustiveAssignementSolver(pb, LC)
    val hunSolver = new HungarianSolver(pb, LC)
    val exAlloc = exSolver.run()
    val hunAlloc = hunSolver.run()
    if (debug) println("Exhaustive solution\n"+exAlloc)
    if (debug) println("Hungarian solution\n"+hunAlloc)
    val exFlow = exAlloc.flowtime()
    val hunFlow = hunAlloc.flowtime()
    if (hunFlow > exFlow) throw new RuntimeException(s"Allocation computed with the hungarian algorithm is not optimal $hunFlow vs $exFlow")
    nbInstances += 1
  }
}