// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and (Dis)Solver with SingleGiftOnly or SingleSwapAndSingleGift
  */
object LargeScaleTest {

  val debug= true

  def main(args: Array[String]): Unit = {

    val rule: SocialRule = args(args.length-2) match {
      case "LCmax" => LCmax
      case "LF" => LF
      case _ => throw new RuntimeException(s"Bad social rule : ${args(0)} ${args(1)} ${args(2)}")
    }

    val nbTasksPerWorker = args(args.length-1).toInt
    val r = scala.util.Random
    val system1 = ActorSystem("Test1" + rule + r.nextInt.toString)
    val system2 = ActorSystem("Test2" + rule + r.nextInt.toString)
    //The Actor system
    val file = s"experiments/data/$rule.csv"
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"m,n," +
      s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
      s"minDistributedSwapSolver$rule,openDistributedSwapSolver$rule,meanDistributedSwapSolver$rule,closedDistributedSwapSolver$rule,maxDistributedSwapSolver$rule," +
      s"minRefSolver$rule,openRefSolver$rule,meanRefSolver$rule,closedRefSolver$rule,maxRefSolver$rule," +
      s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
      s"minDistributedSwapSolverTime,openDistributedSwapSolverTime,meanDistributedSwapSolverTime,closedDistributedSwapSolverTime,maxDistributedSwapSolverTime," +
      s"nbPropose4gift,nbCounterPropose4gift,nbAccept4gift,nbReject4gift,nbWithdraw4gift,nbConfirmGift4gift,nbConfirmSwap4Swap,nbInform4gift," +
      s"nbPropose4swap,nbCounterPropose4swap,nbAccept4swap,nbReject4swap,nbWithdraw4swap,nbConfirmGift4swap,nbConfirmSwap4swap,nbInform4swap\n")
    for (m <- 2 to 500 by 2) {
      val n = nbTasksPerWorker * m
      if (debug) println(s"Test configuration with $m peers and $n tasks")
      val nbPb = 20 // should be x*4
      var (refSolverRule, distributedGiftSolverRule, distributedSwapSolverRule,
      refSolverTime, distributedGiftSolverTime, distributedSwapSolverTime) =
        (List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double]())
      var (
      nbPropose4gift, nbCounterPropose4gift, nbAccept4gift, nbReject4gift, nbWithdraw4gift, nbConfirmGift4gift, nbConfirmSwap4gift, nbInform4gift,
      nbPropose4swap, nbCounterPropose4swap, nbAccept4swap, nbReject4swap, nbWithdraw4swap, nbConfirmGift4swap, nbConfirmSwap4swap, nbInform4swap) =
        (
          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

      for (o <- 1 to nbPb) {
        val pb = MATA.randomProblem(m, n, TaskCorrelated)
        if (debug) println(s"Configuration $o")
        val refSolver = rule match {
          case LCmax => new ECTSolver(pb, rule)
          case LF => new BrunoSolver(pb, rule)
        }
        val distributedGiftSolver: DistributedSolver = new DistributedSolver(pb, rule, SingleGiftOnly, system1)
        val distributedSwapSolver: DistributedSolver = new DistributedSolver(pb, rule, SingleSwapAndSingleGift, system2)
        val refAlloc = refSolver.run()

        val distributedGiftAlloc = distributedGiftSolver.run()
        nbPropose4gift += distributedGiftSolver.nbPropose
        nbCounterPropose4gift += distributedGiftSolver.nbCounterPropose
        nbAccept4gift += distributedGiftSolver.nbAccept
        nbReject4gift += distributedGiftSolver.nbReject
        nbWithdraw4gift += distributedGiftSolver.nbWithdraw
        nbConfirmGift4gift += distributedGiftSolver.nbConfirmGift
        nbConfirmSwap4gift += distributedGiftSolver.nbConfirmSwap
        nbInform4gift += distributedGiftSolver.nbInform

        val distributedSwapAlloc = distributedSwapSolver.run()
        nbPropose4swap += distributedSwapSolver.nbPropose
        nbCounterPropose4swap += distributedSwapSolver.nbCounterPropose
        nbAccept4swap += distributedSwapSolver.nbAccept
        nbReject4swap += distributedSwapSolver.nbReject
        nbWithdraw4swap += distributedSwapSolver.nbWithdraw
        nbConfirmGift4swap += distributedSwapSolver.nbConfirmGift
        nbConfirmSwap4swap += distributedSwapSolver.nbConfirmSwap
        nbInform4swap += distributedSwapSolver.nbInform

        rule match {
          case LCmax =>
            refSolverRule ::= refAlloc.makespan()
            distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
            distributedSwapSolverRule ::= distributedSwapAlloc.makespan()
          case LF =>
            refSolverRule ::= refAlloc.flowtime()
            distributedGiftSolverRule ::= distributedGiftAlloc.flowtime()
            distributedSwapSolverRule ::= distributedSwapAlloc.flowtime()
        }
        distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
        distributedSwapSolverTime ::= distributedSwapSolver.solvingTime
        refSolverTime ::= refSolver.solvingTime
      }

      refSolverRule = refSolverRule.sortWith(_ < _)
      distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
      distributedSwapSolverRule = distributedSwapSolverRule.sortWith(_ < _)

      refSolverTime = refSolverTime.sortWith(_ < _)
      distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
      distributedSwapSolverTime = distributedSwapSolverTime.sortWith(_ < _)

      bw.write(
        m+","+n+","+
          distributedGiftSolverRule.min+","+distributedGiftSolverRule(nbPb / 4)+","+distributedGiftSolverRule(nbPb / 2)+","+distributedGiftSolverRule(nbPb * 3 / 4)+","+distributedGiftSolverRule.max+","+
          distributedSwapSolverRule.min+","+distributedSwapSolverRule(nbPb / 4)+","+distributedSwapSolverRule(nbPb / 2)+","+distributedSwapSolverRule(nbPb * 3 / 4)+","+distributedSwapSolverRule.max+","+
          refSolverRule.min+","+refSolverRule(nbPb / 4)+","+refSolverRule(nbPb / 2)+","+refSolverRule(nbPb * 3 / 4)+","+refSolverRule.max+","+
          distributedGiftSolverTime.min+","+distributedGiftSolverTime(nbPb / 4)+","+distributedGiftSolverTime(nbPb / 2)+","+distributedGiftSolverTime(nbPb * 3 / 4)+","+distributedGiftSolverTime.max+","+
          distributedSwapSolverTime.min+","+distributedSwapSolverTime(nbPb / 4)+","+distributedSwapSolverTime(nbPb / 2)+","+distributedSwapSolverTime(nbPb * 3 / 4)+","+distributedSwapSolverTime.max+","+
          refSolverTime.min+","+refSolverTime(nbPb / 4)+","+refSolverTime(nbPb / 2)+","+refSolverTime(nbPb * 3 / 4)+","+refSolverTime.max+","+
          nbPropose4gift / nbPb+","+nbCounterPropose4gift / nbPb+","+nbAccept4gift / nbPb+","+nbReject4gift / nbPb+","+nbWithdraw4gift / nbPb+","+nbConfirmGift4gift / nbPb+","+nbConfirmSwap4gift / nbPb+","+nbInform4gift / nbPb+","+
          nbPropose4swap / nbPb+","+nbCounterPropose4swap / nbPb+","+nbAccept4swap / nbPb+","+nbReject4swap / nbPb+","+nbWithdraw4swap / nbPb+","+nbConfirmGift4swap / nbPb+","+nbConfirmSwap4swap / nbPb+","+nbInform4swap / nbPb+"\n")
      bw.flush()
    }
    System.exit(0)
  }
}
