// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}
import org.scamata.util.MATAWriter

import sys.process._
import scala.io.Source
import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Approximation algorithm for minimizing the makespan/flowtime
  * @param pb to be solver
  * @param rule to be optimized
  */
class LPSolver(pb : MATA, rule : SocialRule) extends DualSolver(pb, rule) {

  val config: Config = ConfigFactory.load()
  val inputPath: String =config.getString("path.scamata")+"/"+config.getString("path.input")
  val outputPath: String = config.getString("path.scamata")+"/"+config.getString("path.output")
  var lpPath: String = rule match {
    case Cmax =>
      config.getString("path.scamata")+"/"+config.getString("path.cmax")
    case Flowtime =>
      config.getString("path.scamata")+"/"+config.getString("path.flowtime")
  }

  def solve(): Allocation = {
    // 1 - Reformulate the problem
    var startingTime = System.nanoTime()
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
    val allocation : Allocation = Allocation(outputPath, pb)
    postSolvingTime = System.nanoTime() - startingTime
    allocation
  }

}
