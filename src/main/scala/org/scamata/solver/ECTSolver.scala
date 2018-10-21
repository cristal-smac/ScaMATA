// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.scamata.core.{Allocation, MATA}
import org.scamata.util.MATAWriter

import scala.sys.process._

/**
  * Earliest completion time heuristic
  * @param pb to be solver
  * @param rule to be optimized
  */
class ECTSolver(pb : MATA, rule : SocialRule) extends Solver(pb, rule) {

  debug = false

  override def solve(): Allocation = {
    var allocation = new Allocation(pb)
    pb.tasks.foreach{ t =>
      if (debug) println(s"Allocate $t")
      var goal = Double.MaxValue
      var bestCandidate = pb.workers.head
      pb.workers.foreach{ w =>
        val current = rule match{
          case LCmax => allocation.workload(w) + pb.cost(w,t)
          case LC => allocation.delay(w) + pb.cost(w,t)
        }
        if (current < goal){
          goal = current
          bestCandidate = w
        }
      }
      allocation = allocation.update(bestCandidate, allocation.bundle(bestCandidate) + t)
    }
    allocation
  }
}


/**
  * Companion object to test it
  */
object ECTSolver extends App {
  val debug = false
  import org.scamata.example.Toy4x4._
  println(pb)
  val lpSolver = new ECTSolver(pb,LCmax)
  println(lpSolver.run().toString)
}