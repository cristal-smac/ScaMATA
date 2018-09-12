// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and (Dis)Solver with SingleGiftOnly)
  */
object GiftVsDisGift {

  val debug= false

    def main(args: Array[String]): Unit = {
      val criterion = args(0)
      val r = scala.util.Random
      val system = ActorSystem("Test"+criterion+r.nextInt.toString)//The Actor system
      val rule: SocialRule = criterion match {
        case "cmax" => LCmax
        case "flowtime" => LC
        case _ => throw new RuntimeException("The argument is not supported")
      }
      val file = s"experiments/data/${rule}GiftVsDisGift.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minGiftSolver$rule,openGiftSolver$rule,meanGiftSolver$rule,closedGiftSolver$rule,maxGiftSolver$rule," +
        s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
        s"minGiftSolverTime,openGiftSolverTime,meanGiftSolverTime,closedGiftSolverTime,maxGiftSolverTime," +
        s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
        s"dealGift,nbPropose,nbAccept,nbReject,nbWithdraw,nbConfirm,nbInform\n")
      for (m <- 2 to 100) {
        for (n <- 2*m to 10*m) {
          if (debug) println(s"Test configuration with $m peers and $n tasks")
          val nbPb = 100 // should be x*4
          var (giftSolverRule, distributedGiftSolverRule, giftSolverTime, distributedGiftSolverTime,
          deal, nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbInform) =
            (List[Double](), List[Double](), List[Double](), List[Double](),
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          for (o <- 1 to nbPb) {
            val pb = MATA.randomProblem(m, n)
            val giftSolver : CentralizedSolver  = new CentralizedSolver(pb, rule, SingleGiftOnly)
            val distributedGiftSolver : DistributedSolver  = new DistributedSolver(pb, rule, SingleGiftOnly, system)
            val giftAlloc = giftSolver.run()
            deal +=  giftSolver.nbConfirmGift + giftSolver.nbConfirmSwap
            val distributedGiftAlloc = distributedGiftSolver.run()
            nbPropose += distributedGiftSolver.nbPropose
            nbAccept += distributedGiftSolver.nbAccept
            nbReject += distributedGiftSolver.nbReject
            nbWithdraw += distributedGiftSolver.nbWithdraw
            nbConfirm += distributedGiftSolver.nbConfirmGift + distributedGiftSolver.nbConfirmSwap
            nbInform += distributedGiftSolver.nbInform
            rule match {
                case LCmax =>
                  giftSolverRule ::= giftAlloc.makespan()
                  distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
                case LC =>
                  giftSolverRule ::= giftAlloc.flowtime()
                  distributedGiftSolverRule ::= distributedGiftAlloc.flowtime()
            }
            giftSolverTime ::= giftSolver.solvingTime
            distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
          }
          giftSolverRule = giftSolverRule.sortWith(_ < _)
          distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
          giftSolverTime = giftSolverTime.sortWith(_ < _)
          distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,${giftSolverRule.min},${giftSolverRule(nbPb/4)},${giftSolverRule(nbPb/2)},${giftSolverRule(nbPb*3/4)},${giftSolverRule.max}," +
            s"${distributedGiftSolverRule.min},${distributedGiftSolverRule(nbPb/4)},${distributedGiftSolverRule(nbPb/2)},${distributedGiftSolverRule(nbPb*3/4)},${distributedGiftSolverRule.max}," +
            s"${giftSolverTime.min},${giftSolverTime(nbPb/4)},${giftSolverTime(nbPb/2)},${giftSolverTime(nbPb*3/4)},${giftSolverTime.max}," +
            s"${distributedGiftSolverTime.min},${distributedGiftSolverTime(nbPb/4)},${distributedGiftSolverTime(nbPb/2)},${distributedGiftSolverTime(nbPb*3/4)},${distributedGiftSolverTime.max}," +
            s"${deal/nbPb}," +
            s"${nbPropose/nbPb}, ${nbAccept/nbPb}, ${nbReject/nbPb}, ${nbWithdraw/nbPb}, ${nbConfirm/nbPb}, ${nbInform/nbPb}\n")
          bw.flush()
        }
      }
    }
}
