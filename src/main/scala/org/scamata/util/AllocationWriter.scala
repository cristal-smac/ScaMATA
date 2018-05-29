// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import java.io.{BufferedWriter, File, FileWriter}

import com.typesafe.config.ConfigFactory
import org.scamata.core._

import scala.collection.SortedSet

/**
  * Write an allocation to a text file
  * @param pathName of the text file
  * @param allocation to be written
  */
class AllocationWriter(pathName: String, allocation : Allocation) {
  val file = new File(pathName)
  def write() : Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(allocation.toString)
    bw.close()
  }
}

/**
  * TestCmax AllocationWriter
  */
object AllocationWriter extends App {
  val config = ConfigFactory.load()
  import org.scamata.example.toy4x4._
  println(pb)
  val alloc = new Allocation(pb)
  alloc.bundle += ( pb.getWorker("a1") -> SortedSet(pb.getTask("t1")) )
  alloc.bundle += ( pb.getWorker("a2") -> SortedSet(pb.getTask("t2")) )
  alloc.bundle += ( pb.getWorker("a3") -> SortedSet(pb.getTask("t3")) )
  alloc.bundle += ( pb.getWorker("a4") -> SortedSet(pb.getTask("t4")) )
  val writer=new AllocationWriter("examples/toy4x4Rcmax.txt", alloc)
  writer.write()
}