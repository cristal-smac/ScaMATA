// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import akka.actor.ActorSystem
import org.scamata.core.MWTA
import org.scamata.solver.{DistributedGiftSolver,GiftSolver}
import org.scamata.solver.{SocialRule, Cmax, Flowtime}
/**
  * Main app to debug DistributedGiftSolver
  */
object DebugDistributedGiftSolver {
  def main(args: Array[String]): Unit = {
    val rule : SocialRule=Cmax
    val r = scala.util.Random
    val system = ActorSystem("Debug" + r.nextInt.toString) //The Actor system
    for (m <- 3 to 10) {
      for (n <- 2 * m to 10 * m) {
        println(s"DEBUG configuration with $m peers and $n tasks")
        val nbPb = 100
        var (goal, disGoal) = (0.0,0.0)
        var (nbDeal, nbDealDis) = (0.0, 0.0)
        for (o <- 1 to nbPb) {
          println(s"DEBUG configuration $o")
          val pb =  MWTA.randomProblem(m, n)
          val distributedGiftSolver: DistributedGiftSolver = new DistributedGiftSolver(pb, rule, system)
          val giftSolver: GiftSolver = new GiftSolver(pb, Cmax)
          val outcome = giftSolver.run()
          nbDeal += giftSolver.nbConfirm
          val disoutcome = distributedGiftSolver.run()
          nbDealDis += distributedGiftSolver.nbConfirm
          goal += (rule match {
            case Cmax => outcome.makespan()
            case Flowtime => outcome.flowtime()
          })
          disGoal += (rule match {
            case Cmax =>
              disoutcome.makespan()
            case Flowtime => disoutcome.flowtime()
          })
        }
        println(s"$m  $n  ${goal/nbPb}  ${disGoal/nbPb} ${nbDeal/nbPb}  ${nbDealDis/nbPb}")
      }
    }
  }
}
