// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import akka.actor.ActorSystem
import org.scamata.core.MWTA
import org.scamata.solver.{DistributedGiftSolver,GiftSolver}
import org.scamata.solver.{SocialRule, Cmax}

/**
  * Main app to trace DistributedGiftSolver
  */
object DebugDistributedGiftSolver {
  def main(args: Array[String]): Unit = {
    val rule : SocialRule=Cmax
    val r = scala.util.Random
    val system = ActorSystem("Debug" + r.nextInt.toString) //The Actor system
    for (m <- 2 to 100) {
      for (n <- 10 * m to 10* m) {
        println(s"DEBUG configuration with $m peers and $n tasks")
        val nbPb = 10
        var (goal, disGoal) = (0.0,0.0)
        var (nbDeal, nbDealDis) = (0.0, 0.0)
        for (o <- 1 to nbPb) {
          println(s"DEBUG configuration $o")
          val pb =  MWTA.randomProblem(m, n)
          val distributedGiftSolver: DistributedGiftSolver = new DistributedGiftSolver(pb, rule, system)
          val giftSolver: GiftSolver = new GiftSolver(pb, rule)
          val outcome = giftSolver.run()
          nbDeal += giftSolver.nbConfirm
          val disOutcome = distributedGiftSolver.run()
          nbDealDis += distributedGiftSolver.nbConfirm
          goal += outcome.makespan()
          disGoal += disOutcome.makespan()
        }
        println(s"$m  $n  ${goal/nbPb}  ${disGoal/nbPb} ${nbDeal/nbPb}  ${nbDealDis/nbPb}")
      }
    }
  }
}
