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

/**
  * Matrix in Scala
  *
  */
object Matrix{

  /**
    * Print
    * @param matrix is an array of array
    * @tparam T type of content
    * @return string representation
    * */
  def show[T](matrix: Array[Array[T]]): String = matrix.map(_.mkString("[",", ","]")).mkString("\n")

  /**
    * Print
    * @tparam T type of content
    * @param f function
    * @param L line number
    * @param C column number
    * @return string representation
    * */
  def show[T](f: (Integer,Integer) => T, L: Integer, C: Integer): String = {
    (for (i <- 0 until L) yield {
        (for (j <- 0 until C) yield f(i, j).toString).mkString("[",", ","]")
      }).mkString("[\n",",\n","]\n")
    }
}


/**
  * Statistical tools
  */
object Stat{

  /**
    * Return the mean of a random variable
    */
  def mean(values: List[Double]) : Double = values.sum / values.length

  /**
    * Returns the variance of a random variable with a Gaussian distribution (i.e. normally distributed)
    * @param values of the random variable
    */
  def variance(values: List[Double]) : Double = {
    val mean = mean(values)
    values.map(a => math.pow(a - mean, 2)).sum / values.length
  }

  /**
    * Returns the mean and the variance of a random variable with a Gaussian distribution (i.e. normally distributed)
    * @param values of the random variable
    */
  def normal(values: List[Double]) : (Double, Double) = (mean(values) , variance(values))


  /**
    * Returns the statistic t for Welch's t-test
    */
  def statistic(values1: List[Double], values2: List[Double]) : Double = {
    val (mean1, var1) = normal(values1)
    val (mean2, var2) = normal(values2)
    (mean1 - mean2) / (math.sqrt(var1/values1.length + var2/values2.length))
  }


}