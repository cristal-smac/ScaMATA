// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import java.lang

import org.scamata.core._

import scala.collection.SortedSet
import scala.io.Source

/**
  * Build a MWTA Problem object from a text file
  * @param fileName of the MWTA Problem
  *
  */
class MWTAParser(val fileName: String ) {
  val debug = false

  var lineNumber = 0

  var m=0 // number of peers
  var n=0 // number of tasks
  var workers: SortedSet[Worker] = SortedSet[Worker]()
  var tasks: SortedSet[Task] = SortedSet[Task]()
  var cost: Map[(Worker, Task), Double] = Map[(Worker,Task), Double]()
  var pb : MWTA = _

  /**
    * Parse file
    */
  def parse(): MWTA = {
    val bufferedSource = Source.fromFile(fileName)
    for (line <- bufferedSource.getLines) {
      lineNumber += 1
      if (debug) println(s"parse $fileName line$lineNumber: $line")
      if (line.startsWith("//")) { //Drop comment
        if (debug) println(s"parse $fileName line$lineNumber: comment $line")
      } else parseLine(line)
    }
    bufferedSource.close()
    val pb = new MWTA(workers, tasks, cost)
    if (pb.isFullySpecified) pb
    else throw new RuntimeException(s"ERROR parse incomplete preferences")
  }

  /**
    * Parse a line
    * @param line of the file to parse
    */
  def parseLine(line: String) : Unit= {
    val couple = line.split(":").map(_.trim)
    if (couple.length != 2) throw new RuntimeException(s"ERROR parseLine $fileName line$lineNumber: comment $line")
    val (key, value) = (couple(0), couple(1))
    //Firstly, the size (m,n) should be setup as strict positive integer
    if (m == 0 || n == 0) parseSize(key, value)
    //Secondly, the entities should be setup as non-empty
    else if ((m != 0) && (n != 0)){
      if (debug) println(s"parseLine m,n=$m,$n")
      if (workers.isEmpty || tasks.isEmpty) {
        parseEntities(key, value)
      }
      else if (tasks.nonEmpty && workers.nonEmpty){
        pb = new MWTA(workers, tasks, cost)
        parseCosts(key, value)
      }
    }
  }

  /**
    * Parse the size of the MWTA Problem (m,n)
    * @param key of the line
    * @param value of the line
    */
  def parseSize(key: String, value: String): Unit = key match {
    case "m" => m = value.toInt
    case "n" => n = value.toInt
    case _ => throw new RuntimeException("ERROR parseSize" + fileName + " L" + lineNumber + "=" + key + "," + value)
  }

  /**
    * Parse the entities (peers, tasks) of the MWTA Problem
    * @param key of the line
    * @param value of the line
    */
  def parseEntities(key: String, value: String): Unit = key match {
    case "tasks" => parseTasks(value)
    case "workers" => parseAgents(value)
    case _ => throw new RuntimeException(s"ERROR parseEntities $fileName line$lineNumber: $key")
  }

  /**
    * Parse the tasks
    * @param names e.g. a string "t1, t2"
    */
  def parseTasks(names: String): Unit={
    val array:Array[String]=names.split(", ").map(_.trim)
    if (array.length!=n) throw new RuntimeException(s"ERROR parseTasks $fileName line$lineNumber: the number of tasks  $n is wrong: ${array.length}")
    val pattern = """(\w+)\s?""".r
    array.foreach{ str: String =>
      str match {
        case pattern(task) =>
          if (debug) println(s"parseTasks: $task")
          tasks += new Task(task)
        case _ => throw new RuntimeException(s"ERROR parseEntities $fileName line$lineNumber: $str")
      }
    }
  }

  /**
    * Parse the peers
    * @param names e.g. a string "a1, a2, a3"
    */
  def parseAgents(names: String): Unit={
    val array:Array[String]=names.split(", ").map(_.trim)
    array.foreach{ str: String =>
      if (debug) println(s"parseAgents: $str")
      workers+= new Worker(str)
    }
  }

  /**
    * Parse the costs
    * @param key of the line
    * @param value of the line
    */
  def parseCosts(key: String, value: String): Unit = {
    if (debug) println(s"parseCosts $key")
    val source=pb.getWorker(key)
    val couple:Array[String]=value.split(" ").map(_.trim)
    if (couple.length!=2) throw new RuntimeException(s"ERROR parse $fileName line$lineNumber: $value")
    val target = pb.getTask(couple(0))
    val valuation : Double = try {
      couple(1).toDouble }catch{
      case e: NumberFormatException => throw  new RuntimeException(s"ERROR parseCosts $fileName line$lineNumber: ${couple(1)}")
    }
    //The costs can be declared in any order
    cost += ((source, target) -> valuation )
    if (debug) println(s"parsePreference weight: ${cost(source, target)}")
  }
}

/**
  * TestCmax MWTAParser
  */
object MWTAParser extends App {
  val parser =new MWTAParser("examples/toy2x4.txt")
  println(parser.parse()) //Run main
}