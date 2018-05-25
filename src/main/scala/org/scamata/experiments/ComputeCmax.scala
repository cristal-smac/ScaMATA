// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Exhuastive
  */
object ComputeCmax {

  val debug= false

    def main(args: Array[String]): Unit = {
      val rule: SocialRule = Cmax
      val r = scala.util.Random
      val system = ActorSystem("ComputeCmax"+rule+r.nextInt.toString)//The Actor system
      val file = s"experiments/data/min$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minexhaustiveSolver$rule,openexhaustiveSolver$rule,meanexhaustiveSolver$rule,closedexhaustiveSolver$rule,maxexhaustiveSolver$rule," +
      s"mingiftSolver$rule,opengiftSolver$rule,meangiftSolver$rule,closedgiftSolver$rule,maxgiftSolver$rule,\n")
      for (m <- 2 to 100) {
        for (n <- 2*m to 2*m) {
          if (debug) println(s"TestCmax configuration with $m peers and $n tasks")
          val nbPb = 100 // should be x*4
          var (exhaustiveSolverRule, exhaustiveSolverTime, giftSolverRule, giftSolverTime) =
            (List[Double](), List[Double](), List[Double](), List[Double]())
          for (o <- 1 to nbPb) {
            val pb = MWTA.randomProblem(m, n)
            val exhaustiveSolver = new ExhaustiveSolver(pb, rule)
            val exhaustiveAlloc = exhaustiveSolver.run()
            val giftSolver = new GiftSolver(pb, rule)
            val giftAlloc = giftSolver.run()
            exhaustiveSolverRule ::= exhaustiveAlloc.makespan()
            giftSolverRule ::= giftAlloc.makespan()
            exhaustiveSolverTime ::= exhaustiveSolver.solvingTime
            giftSolverTime ::= giftSolver.solvingTime

          }
          exhaustiveSolverRule = exhaustiveSolverRule.sortWith(_ < _)
          exhaustiveSolverTime = exhaustiveSolverTime.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          giftSolverTime = giftSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,"+
              s"${exhaustiveSolverRule.min},${exhaustiveSolverRule(nbPb/4)},${exhaustiveSolverRule(nbPb/2)},${exhaustiveSolverRule(nbPb*3/4)},${exhaustiveSolverRule.max}," +
              s"${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
              s"${exhaustiveSolverTime.min},${exhaustiveSolverTime(nbPb/4)},${exhaustiveSolverTime(nbPb/2)},${exhaustiveSolverTime(nbPb*3/4)},${exhaustiveSolverTime.max}," +
              s"${giftSolverTime.min},${giftSolverTime(nbPb/4)},${giftSolverTime(nbPb/2)},${giftSolverTime(nbPb*3/4)},${giftSolverTime.max}\n")
          bw.flush()
        }
      }
    }
}
