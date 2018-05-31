// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and Dis/GiftSolver
  */
object TestCmax {

  val debug= true

    def main(args: Array[String]): Unit = {
      val rule: SocialRule = LCmax
      val r = scala.util.Random
      val system = ActorSystem("TestCmax"+rule+r.nextInt.toString)//The Actor system
      val file = s"experiments/data/$rule.bis.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minGiftSolver$rule,openGiftSolver$rule,meanGiftSolver$rule,closedGiftSolver$rule,maxGiftSolver$rule," +
        s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
        s"minrandomSolver$rule,openrandomSolver$rule,meanrandomSolver$rule,closedrandomSolver$rule,maxrandomSolver$rule," +
        s"minLpSolver$rule,openLpSolver$rule,meanLpSolver$rule,closedLpSolver$rule,maxLpSolver$rule," +
        s"minGiftSolverTime,openGiftSolverTime,meanGiftSolverTime,closedGiftSolverTime,maxGiftSolverTime," +
        s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
        s"minrandomSolverTime,openrandomSolverTime,meanrandomSolverTime,closedrandomSolverTime,maxrandomSolverTime," +
        s"minLpSolverTime,openLpSolverTime,meanLpSolverTime,closedLpSolverTime,maxLpSolverTime," +
        s"minLpSolverPreTime,openLpSolverPreTime,meanLpSolverPreTime,closedSolverPreTime,maxLpSolverPreTime," +
        s"minLpSolverPostTime,openLpSolverPostTime,meanLpSolverPostTime,closedLpSolverPostTime,maxLpSolverPostTime," +
        s"dealGift,nbPropose,nbAccept,nbReject,nbWithdraw,nbConfirm,nbInform\n")
      for (m <- 2 to 100 by 2) {
        for (n <- 5*m to 5*m) {
          if (debug) println(s"TestCmax configuration with $m peers and $n tasks")
          val nbPb = 20 // should be x*4
          var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, randomSolverRule,
          lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, randomSolverTime, distributedGiftSolverTime,
          deal, nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbCancel, nbInform) =
            (List[Double](), List[Double](), List[Double](), List[Double](),
              List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double](),
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 )
          for (o <- 1 to nbPb) {
            val pb = MWTA.randomProblem(m, n)
            if (debug) println(s"Configuration $o")
            val lpSolver : LPSolver  = new LPSolver(pb,rule)
            val giftSolver : GiftSolver  = new GiftSolver(pb,rule)
            val randomSolver : RandomSolver  = new RandomSolver(pb,rule)
            val distributedGiftSolver : DistributedGiftSolver  = new DistributedGiftSolver(pb, rule, system)
            val lpAlloc =lpSolver.run()
            val giftAlloc = giftSolver.run()
            deal +=  giftSolver.nbConfirm
            val randomAlloc = randomSolver.run()
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
                  randomSolverRule ::= randomAlloc.makespan()
                  distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
                case LC =>
                  lpSolverRule ::= lpAlloc.flowtime()
                  giftSolverRule ::= giftAlloc.flowtime()
                  randomSolverRule ::= randomAlloc.flowtime()
                  distributedGiftSolverRule ::= distributedGiftAlloc.flowtime()
            }
            giftSolverTime ::= giftSolver.solvingTime
            randomSolverTime ::= randomSolver.solvingTime
            distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
            lpSolverTime ::= lpSolver.solvingTime
            lpSolverPreTime ::= lpSolver.preSolvingTime
            lpSolverPostTime ::= lpSolver.postSolvingTime
          }
          lpSolverRule = lpSolverRule.sortWith(_ < _)
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
          randomSolverRule = randomSolverRule.sortWith(_ < _)
          lpSolverTime = lpSolverTime.sortWith(_ < _)
          lpSolverPreTime = lpSolverPreTime.sortWith(_ < _)
          lpSolverPostTime = lpSolverPostTime.sortWith(_ < _)
          giftSolverTime = giftSolverTime.sortWith(_ < _)
          randomSolverTime = randomSolverTime.sortWith(_ < _)
          distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
              s"${distributedGiftSolverRule.min},${distributedGiftSolverRule(nbPb/4)},${distributedGiftSolverRule(nbPb/2)},${distributedGiftSolverRule(nbPb*3/4)},${distributedGiftSolverRule.max}," +
            s"${randomSolverRule.min},${randomSolverRule(nbPb/4)},${randomSolverRule(nbPb/2)},${randomSolverRule(nbPb*3/4)},${randomSolverRule.max}," +
            s"${lpSolverRule.min},${lpSolverRule(nbPb/4)},${lpSolverRule(nbPb/2)},${lpSolverRule(nbPb*3/4)},${lpSolverRule.max}," +
            s"${giftSolverTime.min},${giftSolverTime(nbPb/4)},${giftSolverTime(nbPb/2)},${giftSolverTime(nbPb*3/4)},${giftSolverTime.max}," +
              s"${distributedGiftSolverTime.min},${distributedGiftSolverTime(nbPb/4)},${distributedGiftSolverTime(nbPb/2)},${distributedGiftSolverTime(nbPb*3/4)},${distributedGiftSolverTime.max}," +
            s"${randomSolverTime.min},${randomSolverTime(nbPb/4)},${randomSolverTime(nbPb/2)},${randomSolverTime(nbPb*3/4)},${randomSolverTime.max}," +
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
