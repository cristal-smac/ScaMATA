// Copyright (C) Maxime MORGE 2017
package org.scamata.util

import org.scamata.core._
import org.scamata.solver._

import java.nio.file.{Files, Paths}
import akka.actor.ActorSystem

/**
  * Solve a particular MWTA Problem instance
  * sbt "run org.scaia.util.MWTASolver -v examples/toy2x4.txt examples/toy4x4Cmax.txt"
  * java -jar ScaIA-assembly-X.Y.jar org.scaia.util.asia.MWTASolver r -v examples/toy2x4.txt examples/toy4x4Cmax.txt
  *
  */
object MWTASolver extends App {

  val debug = false
  val system = ActorSystem("MWTASolver") //The Actor system
  val usage =
    """
    Usage: java -jar ScaMATA-assembly-X.Y.jar [-v] inputFilename outputFilename
    The following options are available:
    -v: verbose (false by default)
    -f: LC (LCmax by default)
    -d: distributed (false by default)
  """

  // Default parameters for the solver
  var verbose = false
  var socialRule : SocialRule = LCmax
  var distributed : Boolean = false

  // Default fileNames/path for the input/output
  var inputFilename = new String()
  var outputFilename = new String()

  if (args.length <= 2) {
    println(usage)
    System.exit(1)
  }
  var argList = args.toList.drop(1) // drop Classname
  parseFilenames() // parse filenames
  if (verbose) {
    println(s"inputFile:$inputFilename")
    println(s"output:$outputFilename")
  }
  if (!nextOption(argList)) {
    println(s"ERROR MWTASolver: options cannot be parsed ")
    System.exit(1)
  }

  // fail if options cannot be parsed
  val parser = new MWTAParser(inputFilename)
  val pb = parser.parse() // parse problem
  if (verbose) println(pb)
  if (verbose) println(
    s"""
    Run solver with the following parameters:
    verbose:$verbose
    socialRule:$socialRule
    distributed:$distributed
    ...
  """)
  val solver = selectSolver(pb)
  solver.debug = verbose
  val allocation = solver.run()
  val writer = new AllocationWriter(outputFilename, allocation)
  writer.write()
  socialRule match {
    case LCmax => println(s"Makespan: ${allocation.makespan}")
    case LC => println(s"LC: ${allocation.flowtime}")
  }

  println(s"Processing time: ${solver.solvingTime} (ns)")
  System.exit(0)

  /**
    * Parse filenames at first
    */
  def parseFilenames(): Unit = {
    outputFilename = argList.last.trim
    argList = argList.dropRight(1) // drop outputFile
    inputFilename = argList.last.trim
    argList = argList.dropRight(1) //drop inputFile
    if (!Files.exists(Paths.get(inputFilename)) || Files.exists(Paths.get(outputFilename))) {
      println(s"ERROR parseFilename: either $inputFilename does not exist or $outputFilename already exist")
      System.exit(1)
    }
  }

  /**
    * Parse options at second
    *
    * @param tags is the list of options
    */
  def nextOption(tags: List[String]): Boolean = {
    if (tags.isEmpty) return true
    val tag: String = tags.head.substring(1) // remove '-'
    tag match {
      case "v" => verbose = true
      case "f" => socialRule = LC
      case "d" => distributed = true
      case _ => false
    }
    nextOption(tags.tail)
  }

  /**
    * Returns the solver
    * @param pb MWTA
    */
  def selectSolver(pb: MWTA): Solver = {
    if (distributed) new DistributedGiftSolver(pb, socialRule, system)
    else new GiftSolver(pb, socialRule)
  }
}