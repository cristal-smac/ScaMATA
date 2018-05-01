// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import akka.actor.ActorSystem
import org.scamata.solver.DistributedGiftSolver
import org.scamata.solver.{SocialRule, Cmax, Flowtime}
/**
  * Main app to debug DistributedGiftSolver
  */
object DebugDistributedGiftSolver {
  def main(args: Array[String]): Unit = {
    val r = scala.util.Random
    val system = ActorSystem("Debug" + r.nextInt.toString) //The Actor system
    for (m <- 2 to 10) {
      for (n <- 2 * m to 10 * m) {
        println(s"DEBUG configuration with $m workers and $n tasks")
        val nbPb = 1000
        for (o <- 1 to nbPb) {
          import org.scamata.example.toy4x4._
          //val pb =  MATA.randomProblem(m, n)
          val distributedGiftSolver: DistributedGiftSolver = new DistributedGiftSolver(pb, Cmax, system)
          distributedGiftSolver.run()
          System.out.flush()
          println("----------------------------------------------------------------------------------")
        }
      }
    }
  }
}
