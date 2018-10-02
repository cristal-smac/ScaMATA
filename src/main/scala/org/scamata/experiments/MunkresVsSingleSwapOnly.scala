// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Munkres and (Dis)Solver with SingleSwapOnly)
  */
object MunkresVsSingleSwapOnly{

  val debug= false

    def main(args: Array[String]): Unit = {
      val criterion = args(0)
      val r = scala.util.Random
      val system = ActorSystem("Test"+criterion+r.nextInt.toString)//The Actor system
      val rule = LC

      val file = s"experiments/data/munkresVsSwap.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"n," +
        s"minSwapSolver$rule,openSwapSolver$rule,meanSwapSolver$rule,closedSwapSolver$rule,maxSwapSolver$rule," +
        s"minDistributedSwapSolver$rule,openDistributedSwapSolver$rule,meanDistributedSwapSolver$rule,closedSwapSwapSolver$rule,maxDistributedSwapSolver$rule," +
        s"minSwapSolverTime,openSwapSolverTime,meanSwapSolverTime,closedSwapSolverTime,maxSwapSolverTime," +
        s"minDistributedGSwapSolverTime,openDistributedSwapSolverTime,meanDistributedSwapSolverTime,closedSwapSwapSolverTime,maxDistributedSwapSolverTime," +
        s"dealSwap,nbPropose,nbAccept,nbReject,nbWithdraw,nbConfirm,nbInform\n")
        for (n <- 10 to 100) {
          if (debug) println(s"Test configuration with $n peers and $n tasks")
          val nbPb = 100 // should be x*4
          var (swapSolverRule, distributedSwapSolverRule, swapSolverTime, distributedSwapSolverTime,
          deal, nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbInform) =
            (List[Double](), List[Double](), List[Double](), List[Double](),
              0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          for (o <- 1 to nbPb) {
            val pb = MATA.randomProblem(n, n)
            val swapSolver : CentralizedSolver  = new CentralizedSolver(pb, rule, SingleSwapOnly)
            val distributedSwapSolver : DistributedSolver  = new DistributedSolver(pb, rule, SingleSwapOnly, system)
            val swapAlloc = swapSolver.run()
            deal +=  swapSolver.nbConfirmSwap + swapSolver.nbConfirmSwap
            val distributedSwapAlloc = distributedSwapSolver.run()
            nbPropose += distributedSwapSolver.nbPropose
            nbAccept += distributedSwapSolver.nbAccept
            nbReject += distributedSwapSolver.nbReject
            nbWithdraw += distributedSwapSolver.nbWithdraw
            nbConfirm += distributedSwapSolver.nbConfirmSwap + distributedSwapSolver.nbConfirmSwap
            nbInform += distributedSwapSolver.nbInform

            swapSolverRule ::= swapAlloc.flowtime()
            distributedSwapSolverRule ::= distributedSwapAlloc.flowtime()

            swapSolverTime ::= swapSolver.solvingTime
            distributedSwapSolverTime ::= distributedSwapSolver.solvingTime
          }
          swapSolverRule = swapSolverRule.sortWith(_ < _)
          distributedSwapSolverRule = distributedSwapSolverRule.sortWith(_ < _)
          swapSolverTime = swapSolverTime.sortWith(_ < _)
          distributedSwapSolverTime = distributedSwapSolverTime.sortWith(_ < _)
          bw.write(
            s"$n,${swapSolverRule.min},${swapSolverRule(nbPb/4)},${swapSolverRule(nbPb/2)},${swapSolverRule(nbPb*3/4)},${swapSolverRule.max}," +
            s"${distributedSwapSolverRule.min},${distributedSwapSolverRule(nbPb/4)},${distributedSwapSolverRule(nbPb/2)},${distributedSwapSolverRule(nbPb*3/4)},${distributedSwapSolverRule.max}," +
            s"${swapSolverTime.min},${swapSolverTime(nbPb/4)},${swapSolverTime(nbPb/2)},${swapSolverTime(nbPb*3/4)},${swapSolverTime.max}," +
            s"${distributedSwapSolverTime.min},${distributedSwapSolverTime(nbPb/4)},${distributedSwapSolverTime(nbPb/2)},${distributedSwapSolverTime(nbPb*3/4)},${distributedSwapSolverTime.max}," +
            s"${deal/nbPb}," +
            s"${nbPropose/nbPb}, ${nbAccept/nbPb}, ${nbReject/nbPb}, ${nbWithdraw/nbPb}, ${nbConfirm/nbPb}, ${nbInform/nbPb}\n")
          bw.flush()
        }
      }
    }
}
