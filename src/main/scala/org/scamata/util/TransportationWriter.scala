// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import java.io.{BufferedWriter, File, FileWriter}

import org.scamata.core.MATA

/**
  * Class to write a MATA pb to a Transportation text file
  * @param pathName the output filename
  * @param pb is a MATA Problem
  */
class TransportationWriter(pathName: String, pb : MATA){
  val debug = false

  val file = new File(pathName)
  def write : Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    if (debug) print(pb.toTOPL)
    bw.write(pb.toTOPL)
    bw.close()
  }
}



