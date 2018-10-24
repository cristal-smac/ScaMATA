// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.scamata.core.{Allocation, MATA}
import org.scamata.util.{MATAWriter, TransportationWriter}

import scala.sys.process._

/**
  * Algorithm from "Scheduling independent tasks to reduce mean finishing time" by
  * Bruno, James and Coffman Jr, Edward G and Sethi, Ravi
  * @param pb to be solver
  * @param rule to be optimized
  */
class BrunoSolver(pb : MATA, rule : SocialRule) extends DualSolver(pb, rule) {

  val config: Config = ConfigFactory.load()
  val inputPath: String =config.getString("path.scamata")+"/"+config.getString("path.input")
  val outputPath: String = config.getString("path.scamata")+"/"+config.getString("path.output")
  var lpPath: String = rule match {
    case LF =>
      config.getString("path.scamata")+"/"+config.getString("path.bruno")
    case _ =>
      throw new RuntimeException("Unknown social rule")
  }
  if (rule == SingleSwapOnly) config.getString("path.scamata")+"/"+config.getString("path.assignment")

  override def solve(): Allocation = {
    // 1 - Reformulate the problem
    var startingTime: Long = System.nanoTime()
    val writer=new TransportationWriter(inputPath ,pb)
    writer.write
    preSolvingTime = System.nanoTime() - startingTime

    // 2 - Run the solver
    val command : String= config.getString("path.opl")+" "+
      lpPath+" "+
      inputPath
    if (debug) println(command)
    val success : Int = (command #> new File("/dev/null")).!
    if (success != 0) throw new RuntimeException("BrunoSolver failed")
    // 3 - Reformulate the output
    startingTime = System.nanoTime()
    val allocation : Allocation = Allocation(outputPath, pb)
    postSolvingTime = System.nanoTime() - startingTime
    allocation
  }
}


/**
  * Companion object to test it
  */
object BrunoSolver extends App {
  val debug = true
  import org.scamata.example.Toy4x4._
  if (debug) println(pb)
  val lpSolver = new BrunoSolver(pb,LF)
  lpSolver.debug = true
  println(lpSolver.run().toString)
}