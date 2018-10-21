// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test LPSolver and (Dis)Solver with SingleGiftOnly or SingleSwapAndSingleGift
  */
object Test {

  val debug= true

  def main(args: Array[String]): Unit = {

    val rule: SocialRule = args(args.length-2) match {
      case "LCmax" => LCmax
      case "LC" => LC
      case _ => throw new RuntimeException(s"Bad social rule : ${args(0)} ${args(1)} ${args(2)}")
    }

    val nbTasksPerWorker = args(args.length-1).toInt
      /*rule match {
      case LCmax => 50
      case LC => 50
    }*/
    val r = scala.util.Random
    val system1 = ActorSystem("Test1" + rule + r.nextInt.toString)
    val system2 = ActorSystem("Test2" + rule + r.nextInt.toString)
    //The Actor system
    val file = s"experiments/data/$rule.csv"
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"m,n," +
      s"minGiftSolver$rule,openGiftSolver$rule,meanGiftSolver$rule,closedGiftSolver$rule,maxGiftSolver$rule," +
      s"minDistributedGiftSolver$rule,openDistributedGiftSolver$rule,meanDistributedGiftSolver$rule,closedDistributedGiftSolver$rule,maxDistributedGiftSolver$rule," +
      s"minSwapSolver$rule,openSwapSolver$rule,meanSwapSolver$rule,closedSwapSolver$rule,maxSwapSolver$rule," +
      s"minDistributedSwapSolver$rule,openDistributedSwapSolver$rule,meanDistributedSwapSolver$rule,closedDistributedSwapSolver$rule,maxDistributedSwapSolver$rule," +
      s"minLpSolver$rule,openLpSolver$rule,meanLpSolver$rule,closedLpSolver$rule,maxLpSolver$rule," +
      s"minGiftSolverTime,openGiftSolverTime,meanGiftSolverTime,closedGiftSolverTime,maxGiftSolverTime," +
      s"minDistributedGiftSolverTime,openDistributedGiftSolverTime,meanDistributedGiftSolverTime,closedDistributedGiftSolverTime,maxDistributedGiftSolverTime," +
      s"minSwapSolverTime,openSwapSolverTime,meanSwapSolverTime,closedSwapSolverTime,maxSwapSolverTime," +
      s"minDistributedSwapSolverTime,openDistributedSwapSolverTime,meanDistributedSwapSolverTime,closedDistributedSwapSolverTime,maxDistributedSwapSolverTime," +
      s"minLpSolverTime,openLpSolverTime,meanLpSolverTime,closedLpSolverTime,maxLpSolverTime," +
      s"minLpSolverPreTime,openLpSolverPreTime,meanLpSolverPreTime,closedSolverPreTime,maxLpSolverPreTime," +
      s"minLpSolverPostTime,openLpSolverPostTime,meanLpSolverPostTime,closedLpSolverPostTime,maxLpSolverPostTime," +
      s"gift4gift,gift4swap,swap4swap," +
      s"nbPropose4gift,nbCounterPropose4gift,nbAccept4gift,nbReject4gift,nbWithdraw4gift,nbConfirmGift4gift,nbConfirmSwap4Swap,nbInform4gift," +
      s"nbPropose4swap,nbCounterPropose4swap,nbAccept4swap,nbReject4swap,nbWithdraw4swap,nbConfirmGift4swap,nbConfirmSwap4swap,nbInform4swap\n")
    for (m <- 2 to 200 by 2) {
      val n = nbTasksPerWorker * m
      if (debug) println(s"Test configuration with $m peers and $n tasks")
      val nbPb = 20 // should be x*4
      var (lpSolverRule, giftSolverRule, distributedGiftSolverRule, swapSolverRule, distributedSwapSolverRule,
      lpSolverTime, lpSolverPreTime, lpSolverPostTime, giftSolverTime, swapSolverTime, distributedGiftSolverTime, distributedSwapSolverTime) =
        (List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double](),
          List[Double](), List[Double](), List[Double](), List[Double](), List[Double](), List[Double]())
      var (gift4gift, gift4swap, swap4swap,
      nbPropose4gift, nbCounterPropose4gift, nbAccept4gift, nbReject4gift, nbWithdraw4gift, nbConfirmGift4gift, nbConfirmSwap4gift, nbInform4gift,
      nbPropose4swap, nbCounterPropose4swap, nbAccept4swap, nbReject4swap, nbWithdraw4swap, nbConfirmGift4swap, nbConfirmSwap4swap, nbInform4swap) =
        (0.0, 0.0, 0.0,
          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

      for (o <- 1 to nbPb) {
        val pb = MATA.randomProblem(m, n, MachineTaskCorrelated)
        if (debug) println(s"Configuration $o")
        val lpSolver: ECTSolver = new ECTSolver(pb, rule)
        val giftSolver: CentralizedSolver = new CentralizedSolver(pb, rule, SingleGiftOnly)
        val swapSolver: CentralizedSolver = new CentralizedSolver(pb, rule, SingleSwapAndSingleGift)
        val distributedGiftSolver: DistributedSolver = new DistributedSolver(pb, rule, SingleGiftOnly, system1)
        val distributedSwapSolver: DistributedSolver = new DistributedSolver(pb, rule, SingleSwapAndSingleGift, system2)
        val lpAlloc = lpSolver.run()
        val giftAlloc = giftSolver.run()
        gift4gift += giftSolver.nbPropose
        val swapAlloc = swapSolver.run()
        gift4swap += swapSolver.nbPropose
        swap4swap += swapSolver.nbCounterPropose

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
            lpSolverRule ::= lpAlloc.makespan()
            giftSolverRule ::= giftAlloc.makespan()
            swapSolverRule ::= swapAlloc.makespan()
            distributedGiftSolverRule ::= distributedGiftAlloc.makespan()
            distributedSwapSolverRule ::= distributedSwapAlloc.makespan()
          case LC =>
            lpSolverRule ::= lpAlloc.meanWorkload()
            giftSolverRule ::= giftAlloc.meanWorkload()
            swapSolverRule ::= swapAlloc.meanWorkload()
            distributedGiftSolverRule ::= distributedGiftAlloc.meanWorkload()
            distributedSwapSolverRule ::= distributedSwapAlloc.meanWorkload()
        }
        giftSolverTime ::= giftSolver.solvingTime
        swapSolverTime ::= swapSolver.solvingTime
        distributedGiftSolverTime ::= distributedGiftSolver.solvingTime
        distributedSwapSolverTime ::= distributedSwapSolver.solvingTime
        lpSolverTime ::= lpSolver.solvingTime
        lpSolverPreTime ::= 0.0//lpSolver.preSolvingTime
        lpSolverPostTime ::= 0.0//lpSolver.postSolvingTime
      }

      lpSolverRule = lpSolverRule.sortWith(_ < _)
      giftSolverRule = giftSolverRule.sortWith(_ < _)
      distributedGiftSolverRule = distributedGiftSolverRule.sortWith(_ < _)
      swapSolverRule = swapSolverRule.sortWith(_ < _)
      distributedSwapSolverRule = distributedSwapSolverRule.sortWith(_ < _)

      lpSolverTime = lpSolverTime.sortWith(_ < _)
      lpSolverPreTime = lpSolverPreTime.sortWith(_ < _)
      lpSolverPostTime = lpSolverPostTime.sortWith(_ < _)
      giftSolverTime = giftSolverTime.sortWith(_ < _)
      swapSolverTime = swapSolverTime.sortWith(_ < _)
      distributedGiftSolverTime = distributedGiftSolverTime.sortWith(_ < _)
      distributedSwapSolverTime = distributedSwapSolverTime.sortWith(_ < _)

      bw.write(
        m+","+n+","+giftSolverRule.min+","+giftSolverRule(nbPb / 4)+","+giftSolverRule(nbPb / 2)+","+giftSolverRule(nbPb * 3 / 4)+","+giftSolverRule.max+","+
          distributedGiftSolverRule.min+","+distributedGiftSolverRule(nbPb / 4)+","+distributedGiftSolverRule(nbPb / 2)+","+distributedGiftSolverRule(nbPb * 3 / 4)+","+distributedGiftSolverRule.max+","+
          swapSolverRule.min+","+swapSolverRule(nbPb / 4)+","+swapSolverRule(nbPb / 2)+","+swapSolverRule(nbPb * 3 / 4)+","+swapSolverRule.max+","+
          distributedSwapSolverRule.min+","+distributedSwapSolverRule(nbPb / 4)+","+distributedSwapSolverRule(nbPb / 2)+","+distributedSwapSolverRule(nbPb * 3 / 4)+","+distributedSwapSolverRule.max+","+
          lpSolverRule.min+","+lpSolverRule(nbPb / 4)+","+lpSolverRule(nbPb / 2)+","+lpSolverRule(nbPb * 3 / 4)+","+lpSolverRule.max+","+
          giftSolverTime.min+","+giftSolverTime(nbPb / 4)+","+giftSolverTime(nbPb / 2)+","+giftSolverTime(nbPb * 3 / 4)+","+giftSolverTime.max+","+
          distributedGiftSolverTime.min+","+distributedGiftSolverTime(nbPb / 4)+","+distributedGiftSolverTime(nbPb / 2)+","+distributedGiftSolverTime(nbPb * 3 / 4)+","+distributedGiftSolverTime.max+","+
          swapSolverTime.min+","+swapSolverTime(nbPb / 4)+","+swapSolverTime(nbPb / 2)+","+swapSolverTime(nbPb * 3 / 4)+","+swapSolverTime.max+","+
          distributedSwapSolverTime.min+","+distributedSwapSolverTime(nbPb / 4)+","+distributedSwapSolverTime(nbPb / 2)+","+distributedSwapSolverTime(nbPb * 3 / 4)+","+distributedSwapSolverTime.max+","+
          lpSolverTime.min+","+lpSolverTime(nbPb / 4)+","+lpSolverTime(nbPb / 2)+","+lpSolverTime(nbPb * 3 / 4)+","+lpSolverTime.max+","+
          lpSolverPreTime.min+","+lpSolverPreTime(nbPb / 4)+","+lpSolverPreTime(nbPb / 2)+","+lpSolverPreTime(nbPb * 3 / 4)+","+lpSolverPreTime.max+","+
          lpSolverPostTime.min+","+lpSolverPostTime(nbPb / 4)+","+lpSolverPostTime(nbPb / 2)+","+lpSolverPostTime(nbPb / 4)+","+lpSolverPostTime.max+","+
          gift4gift / nbPb+","+gift4swap / nbPb +","+swap4swap /nbPb+","+
          nbPropose4gift / nbPb+","+nbCounterPropose4gift / nbPb+","+nbAccept4gift / nbPb+","+nbReject4gift / nbPb+","+nbWithdraw4gift / nbPb+","+nbConfirmGift4gift / nbPb+","+nbConfirmSwap4gift / nbPb+","+nbInform4gift / nbPb+","+
          nbPropose4swap / nbPb+","+nbCounterPropose4swap / nbPb+","+nbAccept4swap / nbPb+","+nbReject4swap / nbPb+","+nbWithdraw4swap / nbPb+","+nbConfirmGift4swap / nbPb+","+nbConfirmSwap4swap / nbPb+","+nbInform4swap / nbPb+"\n")
      bw.flush()
    }
    System.exit(0)
  }
}
