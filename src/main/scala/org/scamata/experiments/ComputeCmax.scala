// Copyright (C) Maxime MORGE 2018
package org.scamata.experiments

import java.io.{BufferedWriter, FileWriter}

import akka.actor.ActorSystem
import org.scamata.core._
import org.scamata.solver._

/**
  * Main app to test Exhuastive
  */
object ComputeCmax {

  val debug= false

    def main(args: Array[String]): Unit = {
      val rule: SocialRule = Cmax
      val r = scala.util.Random
      val system = ActorSystem("ComputeCmax"+rule+r.nextInt.toString)//The Actor system
      val file = s"experiments/data/min$rule.csv"
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"m,n," +
        s"minexhaustiveSolver$rule,openexhaustiveSolver$rule,meanexhaustiveSolver$rule,closedexhaustiveSolver$rule,maxexhaustiveSolver$rule,\n")
      for (m <- 2 to 100) {
        for (n <- 2*m to 2*m) {
          if (debug) println(s"TestCmax configuration with $m peers and $n tasks")
          val nbPb = 100 // should be x*4
          var (exhaustiveSolverRule, exhaustiveSolverTime) =
            (List[Double](), List[Double]())
          for (o <- 1 to nbPb) {
            val pb = MWTA.randomProblem(m, n)
            val exhaustiveSolver = new ExhaustiveSolver(pb, rule)
            val exhaustiveAlloc = exhaustiveSolver.run()
            rule match {
                case Cmax =>
                  exhaustiveSolverRule ::= exhaustiveAlloc.makespan()
                case Flowtime =>
                  exhaustiveSolverRule ::= exhaustiveAlloc.flowtime()
            }
            exhaustiveSolverTime ::= exhaustiveSolver.solvingTime
          }
          exhaustiveSolverRule = exhaustiveSolverRule.sortWith(_ < _)
          exhaustiveSolverTime = exhaustiveSolverTime.sortWith(_ < _)
          bw.write(
            s"$m,$n,${exhaustiveSolverRule.min},${exhaustiveSolverRule(nbPb/4)},${exhaustiveSolverRule(nbPb/2)},${exhaustiveSolverRule(nbPb*3/4)},${exhaustiveSolverRule.max}," +
            s"${exhaustiveSolverTime.min},${exhaustiveSolverTime(nbPb/4)},${exhaustiveSolverTime(nbPb/2)},${exhaustiveSolverTime(nbPb*3/4)},${exhaustiveSolverTime.max},\n")
          bw.flush()
        }
      }
    }
}
