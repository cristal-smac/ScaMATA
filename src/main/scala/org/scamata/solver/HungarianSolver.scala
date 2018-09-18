// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}
import util.control.Breaks._
import org.scamata.util.MathUtils._

/**
  * Solver based on tje Munkres' algorithm
  *
  * @param pb   to be solver
  * @param rule to be optimized
  */
class HungarianSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule) {
  debug = true

  val nbRows = Math.min(pb.n(), pb.m())
  val nbCols = Math.max(pb.n(), pb.m())

  // Buid cost matrix
  val cost = Array.ofDim[Double](nbRows, nbCols)
  var (i, j) = (0, 0)
  if (pb.m() <= pb.n()) {
    pb.workers.toList.foreach { a =>
      j = 0
      pb.tasks.toList.foreach { t =>
        cost(i)(j) = pb.cost(a, t)
        j += 1
      }
      i += 1
    }
  } else {
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
  val starred = Array.ofDim[Boolean](nbRows, nbCols)
  for (i <- 0 until nbRows) {
    for (j <- 0 until nbCols) {
      starred(i)(j) = false
    }
  }
  var coveredColumns = Set[Int]()
  var coveredLines = Set[Int]()

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    if (rule != LC) throw new RuntimeException("Hungarian Solver is not suitable")
    if (debug) showCost()
    stepA()
    if (debug) {
      println("Step A")
      showCost()
    }
    stepB()
    if (debug) {
      println("Step B")
      showCost()
    }
    stepC()
    if (debug) {
      println("Step C")
      showCost()
    }
    stepD()
    if (debug) {
      println("Step D")
      showCost()
    }
    if (coveredColumns.size == nbRows) { // If K columns are covered
      if (debug) println("Step D successful")
      return starAllocation() // the starred zeros describe a complete set of unique assignments
    }
    //step next TODO
    Allocation.randomAllocation(pb)
  }

  /**
    * For each row of the matrix, find the smallest element and subtract it from every element in its row.
    */
  def stepA(): Unit = {
    for (i <- 0 until nbRows) {
      var minInRow = cost(i)(0)
      for (j <- 0 until nbCols) {
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
      for (i <- 0 until nbRows) {
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
    return true
  }

  /**
    * Cover each column containing a starred zero.
    */
  def stepD(): Unit = {
    for (j <- 0 until nbCols) {
      breakable {
        for (i <- 0 until nbRows) {
          if (starred(i)(j)) {
            coveredColumns += j
            break()
          }
        }
      }
    }
  }

  /**
    * Returns the allocation based on the starred Z
    */
  def starAllocation(): Allocation = {
    var allocation = new Allocation(pb)
    var (i, j) = (0, 0)
    if (pb.m() <= pb.n()) {
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
    pb.tasks.toList.foreach { t =>
      j = 0
      pb.workers.toList.foreach { a =>
        if (starred(i)(j)) allocation.bundle = allocation.bundle.updated(a, allocation.bundle(a) + t)
        j += 1
      }
      i += 1
    }
    return allocation
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
        print(cost(i)(j)+ strikethrough  + star)
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

