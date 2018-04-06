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
        case _ => throw new RuntimeException("The argument is not suppported")
      }
      val file = s"experiments/data/$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n,giftSolver$rule,distributedGiftSolver$rule,lpSolver$rule,giftSolverTime,distributedGiftSolverTime,lpSolverTime,lpSolverPreTime,lpSolverPostTime\n")
      for (m <- 2 to 10) {
        for (n <- 2 to 100) {
          if (debug) println(s"Test configuration with $m agents and $n tasks")
          val nbPb = 100
          var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, distributedGiftSolverTime) =
            (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          for (o <- 1 to nbPb) {
            val pb = MATA.randomProblem(m, n)
            val lpSolver : LPSolver  = new LPSolver(pb,rule)
            val giftSolver : GiftSolver  = new GiftSolver(pb,rule)
            val distributedGiftSolver : DistributedGiftSolver  = new DistributedGiftSolver(pb, rule, system)
            val lpAlloc =lpSolver.run()
            val giftAlloc = giftSolver.run()
            val distributedGiftAlloc = distributedGiftSolver.run()
            rule match {
                case Cmax =>
                  lpSolverRule += lpAlloc.makespan()
                  giftSolverRule += giftAlloc.makespan()
                  distributedGiftSolverRule += distributedGiftAlloc.makespan()
                case Flowtime =>
                  lpSolverRule += lpAlloc.flowtime()
                  giftSolverRule += giftAlloc.flowtime()
                  distributedGiftSolverRule += distributedGiftAlloc.flowtime()
            }
            giftSolverTime += giftSolver.solvingTime
            distributedGiftSolverTime += distributedGiftSolver.solvingTime
            lpSolverTime += lpSolver.solvingTime
            lpSolverPreTime += lpSolver.preSolvingTime
            lpSolverPostTime += lpSolver.postSolvingTime
          }
          bw.write(s"$m,$n,${giftSolverRule/nbPb},${distributedGiftSolverRule/nbPb},${lpSolverRule/nbPb},${giftSolverTime/nbPb},${distributedGiftSolverTime/nbPb},${lpSolverTime/nbPb},${lpSolverPreTime/nbPb},${lpSolverPostTime/nbPb}\n")
          bw.flush()
        }
      }
    }
}
