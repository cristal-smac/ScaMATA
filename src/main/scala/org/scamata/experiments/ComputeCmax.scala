// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Exhaustive solver vs Gift solver vs ect solver
  */
object ComputeCmax {

  val debug= true

    def main(args: Array[String]): Unit = {
      val rule: SocialRule = LCmax
      val r = scala.util.Random
      val file = s"experiments/data/min$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minexhaustiveSolver$rule,openexhaustiveSolver$rule,meanexhaustiveSolver$rule,closedexhaustiveSolver$rule,maxexhaustiveSolver$rule," +
        s"mingiftSolver$rule,opengiftSolver$rule,meangiftSolver$rule,closedgiftSolver$rule,maxgiftSolver$rule,"+
        s"minswapSolver$rule,openswapSolver$rule,meanswapSolver$rule,closedswapSolver$rule,maswapSolver$rule,"+
        s"minectSolver$rule,openectSolver$rule,meanectSolver$rule,closedectSolver$rule,maxectSolver$rule\n")
      for (m <- 2 to 100) {
        for (n <- 5 to 5) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 20 // should be x*4
          var (exhaustiveSolverRule, giftSolverRule, swapSolverRule, ectSolverRule) =
            (List[Double](), List[Double](),  List[Double](), List[Double]())
          for (o <- 1 to nbPb) {
            if (debug) println(s"Configuration $o")
            val pb = MATA.randomProblem(m, n, TaskCorrelated)
            val exhaustiveSolver = new ExhaustiveSolver(pb, rule)
            val exhaustiveAlloc = exhaustiveSolver.run()
            val giftSolver = new CentralizedSolver(pb, rule, SingleGiftOnly)
            val giftAlloc = giftSolver.run()
            val swapSolver = new CentralizedSolver(pb, rule, SingleSwapAndSingleGift)
            val swapAlloc = swapSolver.run()
            val ectSolver = new ECTSolver(pb, rule)
            val ectAlloc = ectSolver.run()
            exhaustiveSolverRule ::= exhaustiveAlloc.makespan()
            giftSolverRule ::= giftAlloc.makespan()
            swapSolverRule ::= swapAlloc.makespan()
            ectSolverRule ::= ectAlloc.makespan()
          }
          exhaustiveSolverRule = exhaustiveSolverRule.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          swapSolverRule = swapSolverRule.sortWith(_ < _)
          ectSolverRule = ectSolverRule.sortWith(_ < _)
          bw.write(
            s"$m,$n,"+
              s"${exhaustiveSolverRule.min},${exhaustiveSolverRule(nbPb/4)},${exhaustiveSolverRule(nbPb/2)},${exhaustiveSolverRule(nbPb*3/4)},${exhaustiveSolverRule.max}," +
              s"${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
              s"${swapSolverRule.min},${swapSolverRule(nbPb/4)},${swapSolverRule(nbPb/2)},${swapSolverRule(nbPb*3/4)},${swapSolverRule.max}," +
              s"${ectSolverRule.min},${ectSolverRule(nbPb/4)},${ectSolverRule(nbPb/2)},${ectSolverRule(nbPb*3/4)},${ectSolverRule.max}\n")
          bw.flush()
        }
      }
    }
}
