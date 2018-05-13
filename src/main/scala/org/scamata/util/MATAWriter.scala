// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import java.io.{BufferedWriter, File, FileWriter}

import com.typesafe.config.ConfigFactory
import org.scamata.core.MWTA

/**
  * Class to write a MWTA pb to a text file
  * @param pathName the output filename
  * @param pb is a MWTA Problem
  */
class MATAWriter(pathName: String, pb : MWTA){
  val debug = false

  val file = new File(pathName)
  def write : Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    if (debug) print(pb.toOPL)
    bw.write(pb.toOPL)
    bw.close()
  }
}

/**
  * Test MATAWriter
  */
object MATAWriter extends App{
  val config = ConfigFactory.load()
  import org.scamata.example.toy4x4._
  println(pb)
  val writer=new MATAWriter(config.getString("path.scamata")+"/"+config.getString("path.input"),pb)
  writer.write
}

