// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import org.scamata.core._
import org.scamata.solver._

/**
  * Companion object to test it
  */
object TestDelay extends App {
  val debug = false
  import org.scamata.example.AAMAS4x4._
  var a1 = new Allocation(pb)
  a1 =a1.update(w1,a1.bundle(w1) + t1)
  a1 =a1.update(w1,a1.bundle(w1) + t2)
  a1 =a1.update(w2,a1.bundle(w2) + t3)
  a1 =a1.update(w2,a1.bundle(w2) + t4)

  println("w1 : "+a1.delay(w1))
  println("w2 : "+a1.delay(w2))
  println(a1.flowtime())

}