// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import akka.actor.ActorSystem
import org.scamata.core.MATA
import org.scamata.solver.{DistributedGiftSolver,GiftSolver}
import org.scamata.solver.{SocialRule, Cmax, Flowtime}
/**
  * Main app to debug DistributedGiftSolver
  */
object DebugDistributedGiftSolver {
  def main(args: Array[String]): Unit = {
    val r = scala.util.Random
    val system = ActorSystem("Debug" + r.nextInt.toString) //The Actor system
    for (m <- 2 to 10) {
      for (n <- 10 * m to 10 * m) {
        println(s"DEBUG configuration with $m peers and $n tasks")
        val nbPb = 100
        var (makespan, dismakespan) = (0.0,0.0)
        var (nbDeal, nbDealDis) = (0.0, 0.0)
        for (o <- 1 to nbPb) {
          val pb =  MATA.randomProblem(m, n)
          val distributedGiftSolver: DistributedGiftSolver = new DistributedGiftSolver(pb, Cmax, system)
          val giftSolver: GiftSolver = new GiftSolver(pb, Cmax)
          val outcome = giftSolver.run()
          nbDeal += giftSolver.nbConfirm
          val disoutcome = distributedGiftSolver.run()
          nbDealDis += distributedGiftSolver.nbConfirm
          makespan += outcome.makespan()
          dismakespan +=disoutcome.makespan()
        }
        println(s"$m  $n  ${makespan/nbPb}  ${dismakespan/nbPb} ${nbDeal/nbPb}  ${nbDealDis/nbPb}")
      }
    }
  }
}
