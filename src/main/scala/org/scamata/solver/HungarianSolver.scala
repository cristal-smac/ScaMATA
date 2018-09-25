// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}
import util.control.Breaks._
import org.scamata.util.MathUtils._

/**
  * Solver based on the Munkres' algorithm
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class HungarianSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule) {
  debug = false

  // Buid cost matrix
  val nbRows = Math.min(pb.n(), pb.m())
  val nbCols = Math.max(pb.n(), pb.m())
  val cost = Array.ofDim[Double](nbRows, nbCols)
  // Build the starred zero matrix
  val starred = Array.ofDim[Boolean](nbRows, nbCols)
  if (pb.m() <= pb.n()) {
    // There are at least as many columns as rows
    pb.workers.toList.foreach { a =>
      j = 0
      pb.tasks.toList.foreach { t =>
        cost(i)(j) = pb.cost(a, t)
        j += 1
      }
      i += 1
    }
  } else {
    //Rotate the matrix
    pb.tasks.toList.foreach { t =>
      j = 0
      pb.workers.toList.foreach { a =>
        cost(i)(j) = pb.cost(a, t)
        j += 1
      }
      i += 1
    }
  }
  // Build the starred zero matrix
  val primed = Array.ofDim[Boolean](nbRows, nbCols)
  for (i <- 0 until nbRows) {
    for (j <- 0 until nbCols) {
      starred(i)(j) = false
    }
  }
  // Build the primed zero matrix
  for (i <- 0 until nbRows) {
    for (j <- 0 until nbCols) {
      primed(i)(j) = false
    }
  }
  // none value
  val NONE = (-1, -1)
  var (i, j) = (0, 0)
  // Covered lines and columns
  var coveredColumns = Set[Int]()
  var coveredRows = Set[Int]()
  var nbLines = 0

  /**
    * Uncover the row of the element z
    */
  def uncoverRow(z: (Int, Int)): Unit = coveredRows -= z._1

  /**
    * Cover the column of the element z
    */
  def coverColumn(z: (Int, Int)): Unit = coveredColumns += z._2


  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    if (rule != LC) throw new RuntimeException("Hungarian Solver is not suitable")
    if (debug) showCost()
    stepA() //  For each row of the matrix, find the smallest element and subtract it from every element in its row.
    if (debug) {
      println("Step A")
      if (debug) showCost()
    }

    stepB() // For each column of the matrix, find the smallest element and subtract it from every element in its column.
    if (debug) {
      println("Step B")
      if (debug) showCost()
    }

    stepC() // While there exists a zero Z with no starred zero in its row and column do star Z
    if (debug) {
      println("Step C")
      if (debug) showCost()
    }
    stepD() // Cover each column containing a starred zero.
    if (debug) {
      println("Step D")
      if (debug) showCost()
    }
    if (coveredColumns.size == nbRows) { // If K columns are covered
      if (debug) println("Step D successful")
      return starAllocation() // the starred zeros describe a complete set of unique assignments
    }
    stepE() // Cover all unncovered zeros
    starAllocation()
  }

  /**
    * For each row of the matrix, find the smallest element and subtract it from every element in its row.
    */
  def stepA(): Unit = {
    for (i <- 0 until nbRows) {
      var minInRow = cost(i)(0)
      for (j <- 1 until nbCols) {
        if (cost(i)(j) < minInRow) minInRow = cost(i)(j)
      }
      for (j <- 0 until nbCols) {
        cost(i)(j) -= minInRow
      }
    }
  }

  /**
    * For each column of the matrix, find the smallest element and subtract it from every element in its column.
    */
  def stepB(): Unit = {
    for (j <- 0 until nbCols) {
      var minInCol = cost(0)(j)
      for (i <- 1 until nbRows) {
        if (cost(i)(j) < minInCol) minInCol = cost(i)(j)
      }
      for (i <- 0 until nbRows) {
        cost(i)(j) -= minInCol
      }
    }
  }

  /**
    * While there exists a zero Z with no starred zero in its row and column do star Z
    */
  def stepC(): Unit = {
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        if (isZero(i, j) && isSingle(i, j)) {
          starred(i)(j) = true
        }
      }
    }
  }

  /**
    * Returns true if there is no starred zero in row i and column j
    */
  def isSingle(i: Int, j: Int): Boolean = {
    for (row <- 0 until nbRows) {
      if (row != i && isZero(row,j) && starred(row)(j)) return false
    }
    for (col <- 0 until nbCols) {
      if (col != j && isZero(i, col) && starred(i)(col)) return false
    }
    true
  }

  /**
    * Cover each column containing a starred zero.
    */
  def stepD(): Unit = {
    nbLines += coverColumnsWithStarredZero()
  }

  /**
    * Cover each column containing a starred zero and returns the number of covered columns
    */
  def coverColumnsWithStarredZero(): Int = {
    var nbCoveredColumns = 0
    for (j <- 0 until nbCols) {
      breakable {
        for (i <- 0 until nbRows) {
          if (starred(i)(j)) {
            coveredColumns += j
            nbCoveredColumns += 1
            break()
          }
        }
      }
    }
    nbCoveredColumns
  }

  /**
    * Returns the allocation based on the starred Z
    */
  def starAllocation(): Allocation = {
    val allocation = new Allocation(pb)
    var (i, j) = (0, 0)
    if (pb.m() <= pb.n()) {
      // There are at least as many columns as rows
      pb.workers.toList.foreach { a =>
        j = 0
        pb.tasks.toList.foreach { t =>
          if (starred(i)(j)) allocation.bundle = allocation.bundle.updated(a, allocation.bundle(a) + t)
          j += 1
        }
        i += 1
      }
      return allocation
    }
    // The matrix is rotated
    pb.tasks.toList.foreach { t =>
      j = 0
      pb.workers.toList.foreach { a =>
        if (starred(i)(j)) allocation.bundle = allocation.bundle.updated(a, allocation.bundle(a) + t)
        j += 1
      }
      i += 1
    }
    allocation
  }

  /**
    * Cover all unncovered zeros
    */
  def stepE(): Unit = {
    while (true) {
      var z = uncoveredZero()
      if (debug && (z != NONE)) println(s"There is an uncovered zero $z in\n${showCost}")
      if (debug && (z == NONE)) println(s"There is no uncovered zero $z in\n${showCost}")
      while (z != NONE){//E
        prime(z)
        if (debug) println(s"Prime z=$z which is an uncovered zero in\n${showCost}")
        var zstar = starredZeroInRow(z)
        if (zstar != NONE) {
          // F
          if (debug) println(s" there is a starred zero zstar=$zstar in the row of z=$z")
          coverRow(zstar)
          uncoverColumn(zstar)
          if (debug) println(s"Cover row and uncover column of zstar in\n${showCost}")
        } else {//H
          if (debug) println(s" there is no starred zero in the row of z=$z")
          unprime(z)
          star(z)
          if (debug) println(s"Unprime and star z=$z in\n${showCost}")
          zstar = anotherStarredZeroInColumn(z)
          while (zstar != NONE) {
            if (debug) println(s"There exists a starred zero zstar=$zstar in the column of z=$z")
            unstar(zstar)
            z = primedZeroInRow(zstar)
            unprime(z)
            star(z)
            zstar = anotherStarredZeroInColumn(z)
            if (debug) println(s"Unstar zstar, rename the primed zero in the row of zstar z, unprime z and star z in\n${showCost}")
          }
          nbLines += 1
          coveredColumns = Set[Int]()
          coveredRows = Set[Int]()
          coverColumnsWithStarredZero()//nbLines = ???
          if (debug) println(s"Recover starred zeros\n${showCost}")
        }//end H
        z = uncoveredZero()
        if (debug) println(s"There is still an uncovered zero $z in\n${showCost}")
      }//end E
      if (debug) println(s"Nblines $nbLines")
      if (nbLines == nbRows){// I
        if (debug) println(s"Solution found\n${showCost}")
        return
      }
      val h: Double = minimalUncorvedValue()
      if (debug) println(s"minimal uncovered value $h")
      if (h > 0) {// J
        for (i <- 0 until nbRows) {
          if (coveredRows.contains(i)) {
            for (j <- 0 until nbCols) {
              cost(i)(j) += h
            }
          }
        }
        for (j <- 0 until nbCols) {
            if (!coveredColumns.contains(j)) {
              for (i <- 0 until nbRows) {
                cost(i)(j) -= h
              }
            }
          }
        if (debug) println(s"Simplify cost\n${showCost}")
      }// end J
    }//end repeat
  }

  /**
    * Cover the row of the element z
    */
  def coverRow(z: (Int, Int)): Unit = coveredRows += z._1

  /**
    * Uncover the comumn of the element z
    */
  def uncoverColumn(z: (Int, Int)): Unit = coveredColumns -= z._2

  /**
    * Prime the element z in the cost matrix
    */
  def prime(z: (Int, Int)): Unit = {
    primed(z._1)(z._2) = true
  }

  /**
    * Unprime the element z in the cost matrix
    */
  def unprime(z: (Int, Int)): Unit = {
    primed(z._1)(z._2) = false
  }

  /**
    * Star the element z in the cost matrix
    */
  def star(z: (Int, Int)): Unit = {
    starred(z._1)(z._2) = true
  }

  /**
    * Unstar the element z in the cost matrix
    */
  def unstar(z: (Int, Int)): Unit = {
    starred(z._1)(z._2) = false
  }

  /**
    * Returns an uncovered zero if exists, eventually none
    */
  def uncoveredZero(): (Int, Int) = {
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        if (isZero(i, j) && !isCovered(i, j)) return (i, j)
      }
    }
    NONE
  }

  /**
    * Returns true if cost(i)(j) is covered
    */
  def isCovered(i: Int, j: Int): Boolean = coveredRows.contains(i) || coveredColumns.contains(j)

  /**
    * Returns true if cost(i)(j) is zero
    */
  def isZero(i: Int, j: Int): Boolean = cost(i)(j) ~= 0.0

  /**
    * Returns a starred zero in the same row as z if exists, eventually none
    */
  def starredZeroInRow(z: (Int, Int)): (Int, Int) = {
    for (j <- 0 until nbCols) {
      if (j != z._2 & isZero(z._1, j) & starred(z._1)(j)) return (z._1, j)
    }
    NONE
  }

  /**
    * Returns a starred zero in the same column as z if exists, eventually none
    */
  def anotherStarredZeroInColumn(z: (Int, Int)): (Int, Int) = {
    for (i <- 0 until nbRows) {
      if (i != z._1 & isZero(i, z._2) & starred(i)(z._2)) return (i, z._2)
    }
    NONE
  }

  /**
    * Returns the primed zero in the same row of z, throws an exception if there is none
    *
    */
  def primedZeroInRow(z: (Int, Int)): (Int, Int) = {
    for (j <- 0 until nbCols) {
      if (isZero(z._1, j) & primed(z._1)(j)) return (z._1, j)
    }
    throw new RuntimeException(s"The prime zero in the row of $z is not found")
  }

  /**
    * Returns the minimal value which is not covered
    */
  def minimalUncorvedValue(): Double = {
    var min = Double.MaxValue
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        if (!coveredRows.contains(i) && !coveredColumns.contains(j) & cost(i)(j) < min) min = cost(i)(j)
      }
    }
    if (min ~= Double.MaxValue) throw new RuntimeException("No uncoverd element is found")
    min
  }

  /**
    * Show costs
    */
  def showCost(): String = {
    var output =""
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        val strikethrough = (coveredColumns.contains(j) || coveredRows.contains(i)) match {
          case true => "-"
          case false => " "
        }
        val star = starred(i)(j) match {
          case true => "* "
          case false => "  "
        }
        val prime = primed(i)(j) match {
          case true => "' "
          case false => "  "
        }
        output += cost(i)(j) + strikethrough + star + prime
      }
      output += "\n"
    }
    output += "\n"
    output
  }
}

/**
  * Companion object to test it
  */
object HungarianSolver extends App {
  val debug = false

  import org.scamata.example.Toy3x3._

  println(pb)
  val solver = new HungarianSolver(pb, LC)
  println(solver.run().toString)
}
/* TODO
m: 4
n: 4
peers: w1, w2, w3, w4
tasks: t1, t2, t3, t4
w1: t1 703.0
w1: t2 182.0
w1: t3 209.0
w1: t4 338.0
w2: t1 943.0
w2: t2 372.0
w2: t3 970.0
w2: t4 484.0
w3: t1 353.0
w3: t2 68.0
w3: t3 152.0
w3: t4 914.0
w4: t1 497.0
w4: t2 528.0
w4: t3 239.0
w4: t4 271.0
*/