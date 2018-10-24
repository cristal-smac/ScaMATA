// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Exhaustive solver vs Gift solver vs ect solver
  */
object ComputeFlowtime {

  val debug= true

    def main(args: Array[String]): Unit = {
      val rule: SocialRule = LF
      val r = scala.util.Random
      val file = s"experiments/data/min$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minexhaustiveSolver$rule,openexhaustiveSolver$rule,meanexhaustiveSolver$rule,closedexhaustiveSolver$rule,maxexhaustiveSolver$rule," +
        s"mingiftSolver$rule,opengiftSolver$rule,meangiftSolver$rule,closedgiftSolver$rule,maxgiftSolver$rule,"+
        s"minswapSolver$rule,openswapSolver$rule,meanswapSolver$rule,closedswapSolver$rule,maswapSolver$rule,"+
        s"minlcSolver$rule,openlcSolver$rule,meanlcSolver$rule,closedlcSolver$rule,maxlcSolver$rule\n")
      for (m <- 2 to 8) {
        for (n <- 8 to 8) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 20 // should be x*4
          var (exhaustiveSolverRule, giftSolverRule, swapSolverRule, lcSolverRule) =
            (List[Double](), List[Double](),  List[Double](), List[Double]())
          for (o <- 1 to nbPb) {
            if (debug) println(s"Configuration $o")
            val pb = MATA.randomProblem(m, n, TaskCorrelated)
            val exhaustiveSolver = new ExhaustiveSolver(pb, rule)
            val exhaustiveAlloc = exhaustiveSolver.run()
            val giftSolver = new CentralizedSolver(pb, rule, SingleGiftOnly)
            val giftAlloc = giftSolver.run()
            val swapSolver = new CentralizedSolver(pb, rule, SingleSwapAndSingleGift)//  TODO show it is optimal
            val swapAlloc = swapSolver.run()
            val lcSolver = new CentralizedSolver(pb, LC, SingleGiftOnly)// TODO show it does not terminate with Swap
            val lcAlloc = lcSolver.run()
            exhaustiveSolverRule ::= exhaustiveAlloc.flowtime()
            giftSolverRule ::= giftAlloc.flowtime()
            swapSolverRule ::= swapAlloc.flowtime()
            lcSolverRule ::= lcAlloc.flowtime()
          }
          exhaustiveSolverRule = exhaustiveSolverRule.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          swapSolverRule = swapSolverRule.sortWith(_ < _)
          lcSolverRule = lcSolverRule.sortWith(_ < _)
          bw.write(
            s"$m,$n,"+
              s"${exhaustiveSolverRule.min},${exhaustiveSolverRule(nbPb/4)},${exhaustiveSolverRule(nbPb/2)},${exhaustiveSolverRule(nbPb*3/4)},${exhaustiveSolverRule.max}," +
              s"${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
              s"${swapSolverRule.min},${swapSolverRule(nbPb/4)},${swapSolverRule(nbPb/2)},${swapSolverRule(nbPb*3/4)},${swapSolverRule.max},"+
              s"${lcSolverRule.min},${lcSolverRule(nbPb/4)},${lcSolverRule(nbPb/2)},${lcSolverRule(nbPb*3/4)},${lcSolverRule.max}\n")
          bw.flush()
        }
      }
    }
}
