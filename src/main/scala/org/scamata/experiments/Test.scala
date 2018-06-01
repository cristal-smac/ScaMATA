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

  val debug= true

    def main(args: Array[String]): Unit = {
      if (args.length <= 0) throw new RuntimeException("Usage: Test LC|LCmax")
      val rule: SocialRule = args(0) match {
        case "LCmax" => LCmax
        case "LC" => LC
        case _ => throw new RuntimeException("Bad social rule")
      }
      val r = scala.util.Random
      val system = ActorSystem("Test"+rule+r.nextInt.toString)//The Actor system
      val file = s"experiments/data/$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minGiftSolver$rule,openGiftSolver$rule,meanGiftSolver$rule,closedGiftSolver$rule,maxGiftSolver$rule," +
        s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
        s"minSwapSolver$rule,openSwapSolver$rule,meanSwapSolver$rule,closedSwapSolver$rule,maxSwapSolver$rule," +
        s"minLpSolver$rule,openLpSolver$rule,meanLpSolver$rule,closedLpSolver$rule,maxLpSolver$rule," +
        s"minGiftSolverTime,openGiftSolverTime,meanGiftSolverTime,closedGiftSolverTime,maxGiftSolverTime," +
        s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
        s"minSwapSolverTime,openSwapSolverTime,meanSwapSolverTime,closedSwapSolverTime,maxSwapSolverTime," +
        s"minLpSolverTime,openLpSolverTime,meanLpSolverTime,closedLpSolverTime,maxLpSolverTime," +
        s"minLpSolverPreTime,openLpSolverPreTime,meanLpSolverPreTime,closedSolverPreTime,maxLpSolverPreTime," +
        s"minLpSolverPostTime,openLpSolverPostTime,meanLpSolverPostTime,closedLpSolverPostTime,maxLpSolverPostTime," +
        s"dealGift,nbPropose,nbAccept,nbReject,nbWithdraw,nbConfirm,nbInform\n")
      for (m <- 2 to 80 by 2) {
        for (n <- 5*m to 5*m) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 20 // should be x*4
          var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, swapSolverRule,
          lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, swapSolverTime, distributedGiftSolverTime,
          gift, swap, nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbCancel, nbInform) =
            (List[Double](), List[Double](), List[Double](), List[Double](),
              List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double](),
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 )
          for (o <- 1 to nbPb) {
            val pb = MWTA.randomProblem(m, n)
            if (debug) println(s"Configuration $o")
            val lpSolver : LPSolver  = new LPSolver(pb,rule)
            val giftSolver : GiftSolver  = new GiftSolver(pb,rule)
            val swapSolver : SwapSolver  = new SwapSolver(pb,rule)
            val distributedGiftSolver : DistributedGiftSolver  = new DistributedGiftSolver(pb, rule, system)
            val lpAlloc =lpSolver.run()
            val giftAlloc = giftSolver.run()
            gift +=  giftSolver.nbConfirm
            val swapAlloc = swapSolver.run()
            swap +=  swapSolver.nbConfirm
            val distributedGiftAlloc = distributedGiftSolver.run()
            nbPropose += distributedGiftSolver.nbPropose
            nbAccept += distributedGiftSolver.nbAccept
            nbReject += distributedGiftSolver.nbReject
            nbWithdraw += distributedGiftSolver.nbWithdraw
            nbConfirm += distributedGiftSolver.nbConfirm
            nbCancel += distributedGiftSolver.nbCancel
            nbInform += distributedGiftSolver.nbInform
            rule match {
                case LCmax =>
                  lpSolverRule ::=  lpAlloc.makespan()
                  giftSolverRule ::= giftAlloc.makespan()
                  swapSolverRule ::= swapAlloc.makespan()
                  distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
                case LC =>
                  lpSolverRule ::= lpAlloc.flowtime()
                  giftSolverRule ::= giftAlloc.flowtime()
                  swapSolverRule ::= swapAlloc.flowtime()
                  distributedGiftSolverRule ::= distributedGiftAlloc.flowtime()
            }
            giftSolverTime ::= giftSolver.solvingTime
            swapSolverTime ::= swapSolver.solvingTime
            distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
            lpSolverTime ::= lpSolver.solvingTime
            lpSolverPreTime ::= lpSolver.preSolvingTime
            lpSolverPostTime ::= lpSolver.postSolvingTime
          }
          lpSolverRule = lpSolverRule.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
          swapSolverRule = swapSolverRule.sortWith(_ < _)
          lpSolverTime = lpSolverTime.sortWith(_ < _)
          lpSolverPreTime = lpSolverPreTime.sortWith(_ < _)
          lpSolverPostTime = lpSolverPostTime.sortWith(_ < _)
          giftSolverTime = giftSolverTime.sortWith(_ < _)
          swapSolverTime = swapSolverTime.sortWith(_ < _)
          distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
              s"${distributedGiftSolverRule.min},${distributedGiftSolverRule(nbPb/4)},${distributedGiftSolverRule(nbPb/2)},${distributedGiftSolverRule(nbPb*3/4)},${distributedGiftSolverRule.max}," +
            s"${swapSolverRule.min},${swapSolverRule(nbPb/4)},${swapSolverRule(nbPb/2)},${swapSolverRule(nbPb*3/4)},${swapSolverRule.max}," +
            s"${lpSolverRule.min},${lpSolverRule(nbPb/4)},${lpSolverRule(nbPb/2)},${lpSolverRule(nbPb*3/4)},${lpSolverRule.max}," +
            s"${giftSolverTime.min},${giftSolverTime(nbPb/4)},${giftSolverTime(nbPb/2)},${giftSolverTime(nbPb*3/4)},${giftSolverTime.max}," +
              s"${distributedGiftSolverTime.min},${distributedGiftSolverTime(nbPb/4)},${distributedGiftSolverTime(nbPb/2)},${distributedGiftSolverTime(nbPb*3/4)},${distributedGiftSolverTime.max}," +
            s"${swapSolverTime.min},${swapSolverTime(nbPb/4)},${swapSolverTime(nbPb/2)},${swapSolverTime(nbPb*3/4)},${swapSolverTime.max}," +
            s"${lpSolverTime.min},${lpSolverTime(nbPb/4)},${lpSolverTime(nbPb/2)},${lpSolverTime(nbPb*3/4)},${lpSolverTime.max}," +
            s"${lpSolverPreTime.min},${lpSolverPreTime(nbPb/4)},${lpSolverPreTime(nbPb/2)},${lpSolverPreTime(nbPb*3/4)},${lpSolverPreTime.max}," +
            s"${lpSolverPostTime.min},${lpSolverPostTime(nbPb/4)},${lpSolverPostTime(nbPb/2)},${lpSolverPostTime(nbPb/4)},${lpSolverPostTime.max}," +
            s"${gift/nbPb}," +  s"${swap/nbPb}," +
            s"${nbPropose/nbPb}, ${nbAccept/nbPb}, ${nbReject/nbPb}, ${nbWithdraw/nbPb}, ${nbConfirm/nbPb}, ${nbInform/nbPb}\n")
          bw.flush()
        }
      }
    }
}
