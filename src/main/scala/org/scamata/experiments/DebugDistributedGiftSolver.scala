// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import akka.actor.ActorSystem
import org.scamata.core.{MATA, Uncorrelated}
import org.scamata.solver._

/**
  * Main app to trace DistributedSolver
  */
object DebugDistributedGiftSolver {
  def main(args: Array[String]): Unit = {
    val rule : SocialRule=LCmax//LC
    val strategy = SingleSwapAndSingleGift//SingleGiftOnly
    val r = scala.util.Random
    val system = ActorSystem("Trace" + r.nextInt.toString) //The Actor system
    for (m <- 20 to 100) {
      for (n <- 10 * m to 10* m) {
        println(s"DEBUG configuration with $m peers and $n tasks")
        val nbPb = 10
        var (goal, disGoal) = (0.0,0.0)
        var (nbDeal, nbDealDis) = (0.0, 0.0)
        for (o <- 1 to nbPb) {
          println(s"DEBUG configuration $o")
          val pb =  MATA.randomProblem(m, n, Uncorrelated)
          //println(pb)
          val distributedGiftSolver: DistributedSolver = new DistributedSolver(pb, rule, strategy, system)
          distributedGiftSolver.debug = true
          val giftSolver: CentralizedSolver = new CentralizedSolver(pb, rule, strategy)
          val outcome = giftSolver.run()
          nbDeal += giftSolver.nbConfirmGift + giftSolver.nbConfirmSwap
          val disOutcome = distributedGiftSolver.run()
          nbDealDis += distributedGiftSolver.nbConfirmGift + distributedGiftSolver.nbConfirmSwap
          goal += outcome.makespan()
          disGoal += disOutcome.makespan()
        }
        println(s"$m  $n  ${goal/nbPb}  ${disGoal/nbPb} ${nbDeal/nbPb}  ${nbDealDis/nbPb}")
      }
    }
  }
}
