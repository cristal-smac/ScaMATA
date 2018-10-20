// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Munkres and (Dis)Solver with SingleSwapOnly and LC
  */
object TestMunkres {

  val debug= true

  def main(args: Array[String]): Unit = {
    val rule: SocialRule = LC
    val nbTasksPerWorker = 1

    val r = scala.util.Random
    val system = ActorSystem("Test" + rule + r.nextInt.toString)
    //The Actor system
    val file = s"experiments/data/munkres.csv"
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"n," +
      s"minSwapSolver$rule,openSwapSolver$rule,meanSwapSolver$rule,closedSwapSolver$rule,maxSwapSolver$rule," +
      s"minDistributedSwapSolver$rule,openDistributedSwapSolver$rule,meanDistributedSwapSolver$rule,closedDistributedSwapSolver$rule,maxDistributedSwapSolver$rule," +
      s"minLpSolver$rule,openLpSolver$rule,meanLpSolver$rule,closedLpSolver$rule,maxLpSolver$rule," +
      s"minSwapSolverTime,openSwapSolverTime,meanSwapSolverTime,closedswapSolverTime,maxSwapSolverTime," +
      s"minDistributedSwapSolverTime,openDistributedSwapSolverTime,meanDistributedSwapSolverTime,closedDistributedSwapSolverTime,maxDistributedSwapSolverTime," +
      s"minLpSolverTime,openLpSolverTime,meanLpSolverTime,closedLpSolverTime,maxLpSolverTime," +
      s"swap4swap," +
      s"nbPropose4swap,nbCounterPropose4swap,nbAccept4swap,nbReject4swap,nbWithdraw4swap,nbConfirmGift4swap,nbConfirmSwap4swap,nbInform4swap\n")
    for (m <- 2 to 350 by 2) {
      val n = nbTasksPerWorker * m
      if (debug) println(s"Test configuration with $m peers and $n tasks")
      val nbPb = 20 // should be x*4
      var (munkresSolverRule, swapSolverRule, distributedSwapSolverRule,
      munkresSolverTime, swapSolverTime, distributedSwapSolverTime) =
        (List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double]())
      var (swap4swap,
      nbPropose4swap, nbCounterPropose4swap, nbAccept4swap, nbReject4swap, nbWithdraw4swap, nbConfirmGift4swap, nbConfirmSwap4swap, nbInform4swap) =
        (0.0,
          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

      for (o <- 1 to nbPb) {
        val pb = MATA.randomProblem(m, n, Uncorrelated)
        if (debug) println(s"Configuration $o")
        val munkresSolver: MunkresSolver = new MunkresSolver(pb, rule)
        val swapSolver: CentralizedSolver = new CentralizedSolver(pb, rule, SingleSwapOnly)
        val distributedSwapSolver: DistributedSolver = new DistributedSolver(pb, rule, SingleSwapOnly, system)

        val munkresAlloc = munkresSolver.run()
        val swapAlloc = swapSolver.run()
        val distributedSwapAlloc = distributedSwapSolver.run()

        swap4swap += swapSolver.nbCounterPropose
        nbPropose4swap += distributedSwapSolver.nbPropose
        nbCounterPropose4swap += distributedSwapSolver.nbCounterPropose
        nbAccept4swap += distributedSwapSolver.nbAccept
        nbReject4swap += distributedSwapSolver.nbReject
        nbWithdraw4swap += distributedSwapSolver.nbWithdraw
        nbConfirmGift4swap += distributedSwapSolver.nbConfirmGift
        nbConfirmSwap4swap += distributedSwapSolver.nbConfirmSwap
        nbInform4swap += distributedSwapSolver.nbInform

        munkresSolverRule ::= munkresAlloc.meanWorkload()
        swapSolverRule ::= swapAlloc.meanWorkload()
        distributedSwapSolverRule ::= distributedSwapAlloc.meanWorkload()

        if (swapAlloc.meanWorkload() <  munkresAlloc.meanWorkload() || distributedSwapAlloc.meanWorkload() < munkresAlloc.meanWorkload()) throw new RuntimeException(s"Pb with\n $pb")


        swapSolverTime ::= swapSolver.solvingTime
        distributedSwapSolverTime ::= distributedSwapSolver.solvingTime
        munkresSolverTime ::= munkresSolver.solvingTime
      }
      munkresSolverRule = munkresSolverRule.sortWith(_ < _)
      swapSolverRule = swapSolverRule.sortWith(_ < _)
      distributedSwapSolverRule = distributedSwapSolverRule.sortWith(_ < _)

      munkresSolverTime = munkresSolverTime.sortWith(_ < _)
      swapSolverTime = swapSolverTime.sortWith(_ < _)
      distributedSwapSolverTime = distributedSwapSolverTime.sortWith(_ < _)

      bw.write(
        n+","+swapSolverRule.min+","+swapSolverRule(nbPb/4) +","+swapSolverRule(nbPb / 2)+","+swapSolverRule(nbPb * 3 / 4)+","+swapSolverRule.max+","+
          distributedSwapSolverRule.min+","+distributedSwapSolverRule(nbPb / 4)+","+distributedSwapSolverRule(nbPb / 2)+","+distributedSwapSolverRule(nbPb * 3 / 4)+","+distributedSwapSolverRule.max+","+
          +munkresSolverRule.min+","+munkresSolverRule(nbPb / 4)+","+munkresSolverRule(nbPb / 2)+","+munkresSolverRule(nbPb * 3 / 4)+","+munkresSolverRule.max+","+
          +swapSolverTime.min+","+swapSolverTime(nbPb / 4)+","+swapSolverTime(nbPb / 2)+","+swapSolverTime(nbPb * 3 / 4)+","+swapSolverTime.max+"," +
          +distributedSwapSolverTime.min+","+distributedSwapSolverTime(nbPb / 4)+","+distributedSwapSolverTime(nbPb / 2)+","+distributedSwapSolverTime(nbPb * 3 / 4)+","+distributedSwapSolverTime.max+","+
          +munkresSolverTime.min+","+munkresSolverTime(nbPb / 4)+","+munkresSolverTime(nbPb / 2)+","+munkresSolverTime(nbPb * 3 / 4)+","+munkresSolverTime.max+","+
          +swap4swap /nbPb+","+
          +nbPropose4swap / nbPb+","+nbCounterPropose4swap / nbPb+","+nbAccept4swap / nbPb+","+nbReject4swap / nbPb+","+nbWithdraw4swap / nbPb+","+nbConfirmGift4swap / nbPb+","+ nbConfirmSwap4swap/nbPb +","+ nbInform4swap / nbPb+"\n")
      bw.flush()
    }
    System.exit(0)
  }
}
