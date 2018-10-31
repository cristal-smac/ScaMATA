// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.scamata.core.{Allocation, MATA}
import org.scamata.util.MATAWriter

import scala.sys.process._

/**
  * Two-pahse heuristic in which, after applying linear programming to generate a partial schedule the earliest completion time heuristic is applied
  * @param pb to be solver
  * @param rule to be optimized
  */
class LPECTSolver(pb : MATA, rule : SocialRule) extends DualSolver(pb, rule) {

  debug = false
  val config: Config = ConfigFactory.load()
  val inputPath: String =config.getString("path.scamata")+"/"+config.getString("path.input")
  val outputPath: String = config.getString("path.scamata")+"/"+config.getString("path.output")
  var lpPath: String = rule match {
    case LCmax =>
      config.getString("path.scamata")+"/"+config.getString("path.nonintegercmax")
    case LF =>
      config.getString("path.scamata")+"/"+config.getString("path.nonintegerflowtime")
    case _ =>
      throw new RuntimeException("Unknown social rule")
  }
  if (rule == SingleSwapOnly) config.getString("path.scamata")+"/"+config.getString("path.assignment")

  override def solve(): Allocation = {

    // 1 - Reformulate the problem
    var startingTime: Long = System.nanoTime()
    val writer=new MATAWriter(inputPath ,pb)
    writer.write
    preSolvingTime = System.nanoTime() - startingTime
    // 2 - Run the solver
    val command : String= config.getString("path.opl")+" "+
      lpPath+" "+
      inputPath
    if (debug) println(command)
    val success : Int = (command #> new File("/dev/null")).!
    if (success != 0) throw new RuntimeException("LPSolver failed")
    // 3 - Reformulate the output
    startingTime = System.nanoTime()
    var allocation : Allocation = Allocation(outputPath, pb)
    postSolvingTime = System.nanoTime() - startingTime
    if (debug) println(s"First step allocation: $allocation")
    // 4 - Adopt the earlier completion time heuristic
    if (debug) println("Unallocated tasks: "+  allocation.unAllocatedTasks().size +"/"+pb.n())
    allocation.unAllocatedTasks().foreach{ t =>
      if (debug) println(s"Allocate $t")
      var goal = Double.MaxValue
      var bestCandidate = pb.workers.head
      pb.workers.foreach{ w =>
        val workload = allocation.workload(w) + pb.cost(w,t)
        if (workload < goal){
          goal = workload
          bestCandidate = w
        }
      }
      allocation = allocation.update(bestCandidate, allocation.bundle(bestCandidate) + t)
      if (debug) println(s"Second step allocation: $allocation")
    }
    allocation
  }
}


/**
  * Companion object to test it
  */
object LPSECTSolver extends App {
  val debug = false
  import org.scamata.example.Toy4x4._
  println(pb)
  val lpSolver = new LPECTSolver(pb,LCmax)
  println(lpSolver.run().toString)
}