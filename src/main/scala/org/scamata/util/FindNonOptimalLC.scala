// Copyright (C) Maxime MORGE 2018
package org.scamata.util
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Exhaustive solver vs Swap solver with LF
  */
object FindNonOptimalLC {

  val debug= true

  def main(args: Array[String]): Unit = {
    val rule: SocialRule = LF

    for (m <- 2 to 8) {
      for (n <- 2 to 8) {
        if (debug) println(s"Test configuration with $m peers and $n tasks")
        val nbPb = 20 // should be x*4
        for (o <- 1 to nbPb) {
          if (debug) println(s"Configuration $o m $m n $n")
          val pb = MATA.randomProblem(m, n, Uncorrelated)
          val exhaustiveSolver = new ExhaustiveSolver(pb, rule)
          val exhaustiveAlloc = exhaustiveSolver.run()
          val swapSolver = new CentralizedSolver(pb, rule, SingleSwapAndSingleGift)
          val swapAlloc = swapSolver.run()
          if (swapAlloc.flowtime()> exhaustiveAlloc.flowtime()){
            println(pb)
            sys.exit(-1)
          }

        }
      }
    }
  }
}
