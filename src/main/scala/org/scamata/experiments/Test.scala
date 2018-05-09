// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and Dis/GiftSolver
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
      bw.write(s"m,n," +
        s"minGiftSolver$rule,openGiftSolver$rule,meanGiftSolver$rule,closedGiftSolver$rule,maxGiftSolver$rule," +
        s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
        s"minExhauSolver$rule,openExhauSolver$rule,meanExhauSolver$rule,closedExhauSolver$rule,maxExhauSolver$rule," +
        s"minLpSolver$rule,openLpSolver$rule,meanLpSolver$rule,closedLpSolver$rule,maxLpSolver$rule," +
        s"minGiftSolverTime,openGiftSolverTime,meanGiftSolverTime,closedGiftSolverTime,maxGiftSolverTime," +
        s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
        s"minExhauSolverTime,openExhauSolverTime,meanExhauSolverTime,closedExhauSolverTime,maxExhauSolverTime," +
        s"minLpSolverTime,openLpSolverTime,meanLpSolverTime,closedLpSolverTime,maxLpSolverTime," +
        s"minLpSolverPreTime,openLpSolverPreTime,meanLpSolverPreTime,closedSolverPreTime,maxLpSolverPreTime," +
        s"minLpSolverPostTime,openLpSolverPostTime,meanLpSolverPostTime,closedLpSolverPostTime,maxLpSolverPostTime," +
        s"dealGift,nbPropose,nbAccept,nbReject,nbWithdraw,nbConfirm,nbInform\n")
      for (m <- 2 to 100) {
        for (n <- 2*m to 2*m) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 100 // should be x*4
          var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, exhauSolverRule,
          lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, exhauSolverTime, distributedGiftSolverTime,
          deal, nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbInform) =
            (List[Double](), List[Double](), List[Double](), List[Double](),
              List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double](),
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          for (o <- 1 to nbPb) {
            val pb = MATA.randomProblem(m, n)
            if (debug) println(s"PB:\n$pb")
            val lpSolver : LPSolver  = new LPSolver(pb,rule)
            val giftSolver : GiftSolver  = new GiftSolver(pb,rule)
            val exhauSolver : ExhaustiveSolver  = new ExhaustiveSolver(pb,rule)
            val distributedGiftSolver : DistributedGiftSolver  = new DistributedGiftSolver(pb, rule, system)
            val lpAlloc =lpSolver.run()
            val giftAlloc = giftSolver.run()
            deal +=  giftSolver.nbConfirm
            if (debug) println(s"GIFT:\n$giftAlloc")
            val exhauAlloc = exhauSolver.run()
            val distributedGiftAlloc = distributedGiftSolver.run()
            nbPropose += distributedGiftSolver.nbPropose
            nbAccept += distributedGiftSolver.nbAccept
            nbReject += distributedGiftSolver.nbReject
            nbWithdraw += distributedGiftSolver.nbWithdraw
            nbConfirm += distributedGiftSolver.nbConfirm
            nbInform += distributedGiftSolver.nbInform
            if (debug) println(s"DISGIFT:\n$distributedGiftAlloc")
            rule match {
                case Cmax =>
                  lpSolverRule ::=  lpAlloc.makespan()
                  giftSolverRule ::= giftAlloc.makespan()
                  exhauSolverRule ::= exhauAlloc.makespan()
                  distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
                case Flowtime =>
                  lpSolverRule ::= lpAlloc.flowtime()
                  giftSolverRule ::= giftAlloc.flowtime()
                  exhauSolverRule ::= exhauAlloc.flowtime()
                  distributedGiftSolverRule ::= distributedGiftAlloc.flowtime()
            }
            giftSolverTime ::= giftSolver.solvingTime
            exhauSolverTime ::= exhauSolver.solvingTime
            distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
            lpSolverTime ::= lpSolver.solvingTime
            lpSolverPreTime ::= lpSolver.preSolvingTime
            lpSolverPostTime ::= lpSolver.postSolvingTime
          }
          lpSolverRule = lpSolverRule.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
          exhauSolverRule = exhauSolverRule.sortWith(_ < _)
          lpSolverTime = lpSolverTime.sortWith(_ < _)
          lpSolverPreTime = lpSolverPreTime.sortWith(_ < _)
          lpSolverPostTime = lpSolverPostTime.sortWith(_ < _)
          giftSolverTime = giftSolverTime.sortWith(_ < _)
          exhauSolverTime = exhauSolverTime.sortWith(_ < _)
          distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
            s"${distributedGiftSolverRule.min},${distributedGiftSolverRule(nbPb/4)},${distributedGiftSolverRule(nbPb/2)},${distributedGiftSolverRule(nbPb*3/4)},${distributedGiftSolverRule.max}," +
            s"${exhauSolverRule.min},${exhauSolverRule(nbPb/4)},${exhauSolverRule(nbPb/2)},${exhauSolverRule(nbPb*3/4)},${exhauSolverRule.max}," +
            s"${lpSolverRule.min},${lpSolverRule(nbPb/4)},${lpSolverRule(nbPb/2)},${lpSolverRule(nbPb*3/4)},${lpSolverRule.max}," +
            s"${giftSolverTime.min},${giftSolverTime(nbPb/4)},${giftSolverTime(nbPb/2)},${giftSolverTime(nbPb*3/4)},${giftSolverTime.max}," +
            s"${distributedGiftSolverTime.min},${distributedGiftSolverTime(nbPb/4)},${distributedGiftSolverTime(nbPb/2)},${distributedGiftSolverTime(nbPb*3/4)},${distributedGiftSolverTime.max}," +
            s"${exhauSolverTime.min},${exhauSolverTime(nbPb/4)},${exhauSolverTime(nbPb/2)},${exhauSolverTime(nbPb*3/4)},${exhauSolverTime.max}," +
            s"${lpSolverTime.min},${lpSolverTime(nbPb/4)},${lpSolverTime(nbPb/2)},${lpSolverTime(nbPb*3/4)},${lpSolverTime.max}," +
            s"${lpSolverPreTime.min},${lpSolverPreTime(nbPb/4)},${lpSolverPreTime(nbPb/2)},${lpSolverPreTime(nbPb*3/4)},${lpSolverPreTime.max}," +
            s"${lpSolverPostTime.min},${lpSolverPostTime(nbPb/4)},${lpSolverPostTime(nbPb/2)},${lpSolverPostTime(nbPb/4)},${lpSolverPostTime.max}," +
            s"${deal/nbPb}," +
            s"${nbPropose/nbPb}, ${nbAccept/nbPb}, ${nbReject/nbPb}, ${nbWithdraw/nbPb}, ${nbConfirm/nbPb}, ${nbInform/nbPb}\n")
          bw.flush()
        }
      }
    }
}
