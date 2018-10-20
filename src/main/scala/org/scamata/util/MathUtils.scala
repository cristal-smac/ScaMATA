// Copyright (C) Maxime MORGE 2018
package org.scamata.util

import scala.collection.SortedSet

/**
  * Compare floating-point numbers in Scala
  *
  */
object MathUtils {
  implicit class MathUtils(x: Double) {
    val precision = 0.000001
    def ~=(y: Double): Boolean = {
      if ((x - y).abs < precision) true else false
    }
  }
}

/**
  * Random weight in Scala
  *
  */
object RandomUtils {
  val r : scala.util.Random = scala.util.Random
  /**
    *  Returns a pseudo-randomly generated Double in ]0;1]
    */
  def strictPositiveWeight() : Double = {
    val number = r.nextDouble() // in [0.0;1.0[
    1.0 - number
  }

  /**
    *  Returns a pseudo-randomly generated Double in  [-1.0;1.0[
    */
  def weight() : Double = {
    val number = r.nextDouble() // in [0.0;1.0[
    number*2-1
  }

  /**
    * Returns a random element in a set
    */
  def random[T](s: SortedSet[T]): T = {
    val n = util.Random.nextInt(s.size)
    s.iterator.drop(n).next
  }

  /**
    * Returns a pseudo-randomly generated Double in [min, max]
    */
  def random(min : Int, max : Int) : Double = {
    (min + util.Random.nextInt((max - min) + 1)).toDouble
  }
}