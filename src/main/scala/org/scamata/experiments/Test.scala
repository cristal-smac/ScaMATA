// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and GiftSolver
  */
object Test {

  val debug= false

    def main(args: Array[String]): Unit = {
      val criterion = args(0)
      val r = scala.util.Random
      val system = ActorSystem("Test"+criterion+r.nextInt.toString)//The Actor system
      val rule: SocialRule = criterion match {
        case "cmax" => Cmax
        case "flowtime" => Flowtime
        case _ => throw new RuntimeException("The argument is not supported")
      }
      val file = s"experiments/data/$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n,giftSolver$rule,distributedGiftSolver$rule,swapSolver$rule,lpSolver$rule,giftSolverTime,distributedGiftSolverTime,swapSolverTime,lpSolverTime,lpSolverPreTime,lpSolverPostTime,dealGift,disDisGift\n")
      for (m <- 2 to 100) {
        for (n <- 10*m to 10*m) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 100
          var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, swapSolverRule, lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, swapSolverTime, distributedGiftSolverTime, deal, deaDis) =
            (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          for (o <- 1 to nbPb) {
            val pb = MATA.randomProblem(m, n)
            if (debug) println(s"PB:\n$pb")
            val lpSolver : LPSolver  = new LPSolver(pb,rule)
            val giftSolver : GiftSolver  = new GiftSolver(pb,rule)
            val swapSolver : SwapSolver  = new SwapSolver(pb,rule)
            val distributedGiftSolver : DistributedGiftSolver  = new DistributedGiftSolver(pb, rule, system)
            val lpAlloc =lpSolver.run()
            val giftAlloc = giftSolver.run()
            deal += giftSolver.nbDeal
            if (debug) println(s"GIFT:\n$giftAlloc")
            val swapAlloc = swapSolver.run()
            val distributedGiftAlloc = distributedGiftSolver.run()
            deaDis += distributedGiftSolver.nbDeal
            if (debug) println(s"DISGIFT:\n$distributedGiftAlloc")
            rule match {
                case Cmax =>
                  lpSolverRule += lpAlloc.makespan()
                  giftSolverRule += giftAlloc.makespan()
                  swapSolverRule += swapAlloc.makespan()
                  distributedGiftSolverRule += distributedGiftAlloc.makespan()
                case Flowtime =>
                  lpSolverRule += lpAlloc.flowtime()
                  giftSolverRule += giftAlloc.flowtime()
                  swapSolverRule += swapAlloc.flowtime()
                  distributedGiftSolverRule += distributedGiftAlloc.flowtime()
            }
            giftSolverTime += giftSolver.solvingTime
            swapSolverTime += swapSolver.solvingTime
            distributedGiftSolverTime += distributedGiftSolver.solvingTime
            lpSolverTime += lpSolver.solvingTime
            lpSolverPreTime += lpSolver.preSolvingTime
            lpSolverPostTime += lpSolver.postSolvingTime
          }
          bw.write(s"$m,$n,${giftSolverRule/nbPb},${distributedGiftSolverRule/nbPb},${swapSolverRule/nbPb},${lpSolverRule/nbPb}," +
            s"${giftSolverTime/nbPb},${distributedGiftSolverTime/nbPb},${swapSolverTime/nbPb},${lpSolverTime/nbPb},${lpSolverPreTime/nbPb},${lpSolverPostTime/nbPb}," +
            s"${deal/nbPb},${deaDis/nbPb}\n")
          bw.flush()
        }
      }
    }
}
