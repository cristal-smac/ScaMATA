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
  debug = true

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
  // Build the primed zero matrix
  val primed = Array.ofDim[Boolean](nbRows, nbCols)
  for (i <- 0 until nbRows) {
    for (j <- 0 until nbCols) {
      starred(i)(j) = false
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
  for (i <- 0 until nbRows) {
    for (j <- 0 until nbCols) {
      primed(i)(j) = false
    }
  }

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    if (rule != LC) throw new RuntimeException("Hungarian Solver is not suitable")
    if (debug) showCost()
    stepA() //  For each row of the matrix, find the smallest element and subtract it from every element in its row.
    if (debug) {
      println("Step A")
      showCost()
    }
    stepB() // For each column of the matrix, find the smallest element and subtract it from every element in its column.
    if (debug) {
      println("Step B")
      showCost()
    }
    stepC() // While there exists a zero Z with no starred zero in its row and column do star Z
    if (debug) {
      println("Step C")
      showCost()
    }
    stepD() // Cover each column containing a starred zero.
    if (debug) {
      println("Step D")
      showCost()
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
        if ((cost(i)(j) ~= 0.0) && isSingle(i, j)) {
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
      if (row != i && (cost(row)(j) ~= 0.0) && starred(row)(j)) return false
    }
    for (col <- 0 until nbCols) {
      if (col != j && (cost(i)(col) ~= 0.0) && starred(i)(col)) return false
    }
    true
  }

  /**
    * Cover each column containing a starred zero.
    */
  def stepD(): Unit = {
    nbLines += coverColumnWithStarredZero()
  }

  /**
    * Cover each column containing a starred zero and returns the number of covered columns
    */
  def coverColumnWithStarredZero(): Int = {
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
      while (z != NONE) {
        prime(z)
        var zstar = starredZeroInRow(z)
        if (zstar != NONE) {
          // There exists a starred zero in the same row of z
          coverRow(zstar) // i.e. row of z
          uncoverColumn(zstar)
        } else {
          unprime(z)
          star(z)
          zstar = starredZeroInColumn(z)
          while (zstar != NONE) {
            // There exists a starred zero in the same column of z
            unstar(zstar)
            z = primedZeroInRow(zstar)
            unprime(z)
            star(z)
            zstar = starredZeroInColumn(z)
          }
          nbLines += 1
          coveredColumns = Set[Int]()
          coveredRows = Set[Int]()
          coverColumnWithStarredZero()
        }
        z = uncoveredZero()
      }
      if (nbLines == nbRows) return
      val h: Double = minimalUncorvedValue()
      for (i <- 0 until nbRows) {
        for (j <- 0 until nbCols) {
          if (coveredRows.contains(i)) cost(i)(j) += h
        }
      }
    }
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
      for (j <- 1 until nbCols) {
        if (isZero(i, j) && !isCovered(i, j)) return (i, j)
      }
    }
    NONE
  }

  /**
    * Returns true if cost(i)(j) is covered
    */
  def isCovered(i: Int, j: Int): Boolean = coveredRows.contains(i) || coveredColumns(j)

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
  def starredZeroInColumn(z: (Int, Int)): (Int, Int) = {
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
    * Returns the minimal walue which is not covered
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
    def showCost(): Unit = {
      for (i <- 0 until nbRows) {
        for (j <- 0 until nbCols) {
          val strikethrough = coveredColumns(j) match {
            case true => "\u0336"
            case false => ""
          }
          val star = starred(i)(j) match {
            case true => "* "
            case false => "  "
          }
          print(cost(i)(j) + strikethrough + star)
        }
        print("\n")
      }
      print("\n")
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

