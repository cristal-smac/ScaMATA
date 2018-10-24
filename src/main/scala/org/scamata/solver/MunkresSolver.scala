// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}
import org.scamata.util.MathUtils._
import util.control.Breaks._

/**
  * Solver based on the Munkres' algorithm also called the Hungarian algorithm or the Kuhn-Munkres algorithm
  * for solving the Assignment Problem in O(n&#94;3) time
  * @param pb   to be solver
  * @param rule to be optimized
  */
class MunkresSolver(pb: MATA, rule: SocialRule) extends Solver(pb, rule) {
  debug = false

  val NONE = (-1, -1)// not in the cost matrix
  var zero0 = (0,0)// first cell
  if (pb.n() !=  pb.m()) throw new RuntimeException("The problem must have the same number of agents and tasks")

  // Build cost matrix
  val n= pb.n()
  val cost = Array.ofDim[Double](n, n)
  buildCostMatrix()

  // Build the marked matrix : 0 if none, 1 if starred,  2 if primed
  val marked = Array.fill[Int](n, n)(elem = 0)

  // Covered lines and columns, initially none
  var coveredColumns = Array.fill[Boolean](n)(elem = false)
  var coveredRows =  Array.fill[Boolean](n)(elem = false)

  //  Series of alternating primed and starred zeros
  var path =  Array.fill[(Int, Int)](2* n)(elem =  (0,0))

  /**
    * Build cost matrix
    */
  def buildCostMatrix() : Unit = {
    var (i, j) = (0, 0)
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
  }


  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    if (rule != LF) throw new RuntimeException("Hungarian Solver is not suitable")
    var done = false
    var step = 1
    while (!done) {
      if (debug) println(showCost())
      if (debug)  println(s"Step $step")
      step = step match {
        case 1 =>
          if (debug) println("For each row of the matrix, find the smallest element and subtract it from every element in its row")
          step1()
        case 2 =>
          if (debug) println("While there exists a zero Z with no starred zero in its row and column do star Z")
          step2()
        case 3 =>
          if (debug) println("Cover each column containing a starred zero")
          step3()
        case 4 =>
          if (debug) println("Find a noncovered zero and prime it")
          step4()
        case 5 =>
          if (debug) println("Construct a series of alternating primed and starred zeros")
          step5()
        case 6 =>
          if (debug) println("Alter cost according to the minimal value")
          step6()
        case 7 => // final step
          done = true
          7
        case _ =>
          throw new RuntimeException("Error in Munkres' algorithm")
      }
    }
    starAllocation()
  }


  /**
    * For each row of the matrix, find the smallest element and subtract it from every element in its row.
    */
  def step1(): Int = {
    for (i <- 0 until n) {
      var minInRow = cost(i)(0)
      for (j <- 1 until n) {
        if (cost(i)(j) < minInRow) minInRow = cost(i)(j)
      }
      for (j <- 0 until n) {
        cost(i)(j) -= minInRow
      }
    }
    2
  }

  /**
    * TODO For each column of the matrix, find the smallest element and subtract it from every element in its column.
    */
  def step1bis(): Int = {
    for (j <- 0 until n) {
      var minInCol = cost(0)(j)
      for (i <- 1 until n) {
        if (cost(i)(j) < minInCol) minInCol = cost(i)(j)
      }
      for (i <- 0 until n) {
        cost(i)(j) -= minInCol
      }
    }
    2
  }

  /**
    * While there exists a zero Z with no starred zero in its row and column do star Z
    */
  def step2(): Int = {
    for (i <- 0 until n) {
      breakable{
        for (j <- 0 until n) {
          if (isZero(i, j) && ! coveredRows(i) && ! coveredColumns(j)) {
            marked(i)(j) = 1
            coveredRows(i) = true
            coveredColumns(j) = true
            break
          }
        }
      }
    }
    coveredColumns = Array.fill[Boolean](n)(elem = false)
    coveredRows =  Array.fill[Boolean](n)(elem = false)
    3
  }

  /**
    * Cover each column containing a starred zero
    * either n columns are covered, we have done
    * or got to step 4
    */
  def step3(): Int = {
    var nbCoveredColumns = 0
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        if (marked(i)(j) == 1 && !coveredColumns(j)) {
          coveredColumns(j) = true
          nbCoveredColumns += 1
        }
      }
    }
    if (nbCoveredColumns >= n) return 7
    4
  }


  /**
    * Find a noncovered zero and prime it.
    * If there is no starred zero in the row containing this primed zero, Go to Step 5.
    * Otherwise, cover this row and uncover the column containing the starred zero.
    * Continue in this manner until there are no uncovered zeros left. Save the smallest uncovered value and Go to Step 6.
    */
  def step4() : Int = {
    var step = 0
    var (i,j)= NONE
    var found = false
    while (! found) {
      val z = uncoveredZero(i0 = 0,j0 = 0)
      if (z == NONE) {
        if (debug) println(s"There are no more uncovered zeros")
        found = true
        step = 6
      } else {
        if (debug) println(s"$z is a non covered zero")
        marked(z._1)(z._2) = 2 // Prime z
        val zstar = starredZeroInRow(z._1)
        if (zstar != NONE) {
          coveredRows(z._1) = true
          coveredColumns(zstar._2) = false
        } else {
          found = true
          zero0 = z
          step = 5
        }
      }
    }
    return step // useless
  }

  /**
    * Construct a series of alternating primed and starred zeros as follows.
    * Let Z0 represent the uncovered primed zero found in Step 4.
    * Let Z1 denote the starred zero in the column of Z0 (if any).
    * Let Z2 denote the primed zero in the row of Z1 (there will always be one).
    * Continue until the series terminates at a primed zero that has no starred zero in its column.
    * Unstar each starred zero of the series, star each primed zero of the series, erasePrime all primes and uncover every line in the matrix.
    * Return to Step 3.
    *
    */
  def step5() : Int = {
    var count = 0
    path(count) = zero0
    if (debug) println(s"Add starred z0 $zero0 in path")
    var finished = false
    while (! finished){
      if (debug) println(path)
      val z1 = starredZeroInColumn(path(count)._2)
      if (z1 != NONE){
        count +=1
        if (debug) println(s"Add starred $z1 in path")
        path(count) = z1
      }
      else {
        finished = true
      }
      if (! finished){
        val z2 = primedZeroInRow(path(count)._1)
        count += 1
        if (debug) println(s"Add primed $z2 in path")
        path(count) = z2
      }
    }
    convert(path, count)
    clear()
    erasePrime()
    3
  }

  /**
    * Add the value found in Step 4 to every element of each covered
    * row, and subtract it from every element of each uncovered column.
    * Return to Step 4 without altering any stars, primes, or covered
    * lines.
    */
  def step6() : Int = {
    val h = minimalUncorvedValue()
    var nbMoves = 0
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        if (coveredRows(i)){
          cost(i)(j) += h
          nbMoves +=1
        }
        if (! coveredColumns(j)){
          cost(i)(j) -= h
          nbMoves +=1
        }
        if (coveredRows(i) && ! coveredColumns(j)){
          nbMoves -=2
        }
      }
    }
    if (nbMoves == 0) throw new RuntimeException("Problem cannot be solved !")
    4
  }

  /**
    * Returns an uncovered zero if exists, eventually none
    */
  def uncoveredZero(): (Int, Int) = {
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        if (isZero(i, j) && !coveredRows(i) && !coveredColumns(j) ) return (i, j)
      }
    }
    NONE
  }


  /**
    * Returns an uncovered zero if exists, eventually none from (i0,j0)
    */
  def uncoveredZero(i0 : Int = 0, j0 : Int = 0): (Int, Int) = {
    var (r, c) = NONE
    var i = i0
    var done = false
    while (!done) {
      breakable {
        var j = j0
        while (true) {
          if (isZero(i, j) && !coveredRows(i) && !coveredColumns(j)) {
            r = i
            c = j
            done = true
          }
          j = (j + 1) % n
          if (j == j0) break()
        }
      }
      i = (i + 1) % n
      if (i == i0) done = true
    }
    (r,c)
  }

  /**
    * Returns the allocation based on the starred zeros
    */
  def starAllocation(): Allocation = {
    val allocation = new Allocation(pb)
    var (i, j) = (0, 0)
    // There are at least as many columns as rows
    pb.workers.toList.foreach { a =>
      j = 0
      pb.tasks.toList.foreach { t =>
        if (marked(i)(j) == 1) allocation.bundle = allocation.bundle.updated(a, allocation.bundle(a) + t)
        j += 1
      }
      i += 1
    }
    if (! allocation.isSingle) throw new RuntimeException("More than one task is allocated to an agent")
    allocation
  }

  /**
    * Returns true if cost(i)(j) is zero
    */
  def isZero(i: Int, j: Int): Boolean = cost(i)(j) ~= 0.0


  /**
    * Returns the first starred zero in the row i if exists, eventually none
    */
  def starredZeroInRow(i: Int): (Int, Int) = {
    for (j <- 0 until n) {
      if (isZero(i, j) && marked(i)(j) == 1 ) return (i, j)
    }
    NONE
  }

  /**
    * Returns the first starred zero in the same column as j if exists, eventually none
    */
  def starredZeroInColumn(j : Int): (Int, Int) = {
    for (i <- 0 until n) {
      if (isZero(i, j) && marked(i)(j) == 1 ) return (i, j)
    }
    NONE
  }

  /**
    * Returns the first primed zero in the row i which necessary exists
    *
    */
  def primedZeroInRow(i:  Int): (Int, Int) = {
    for (j <- 0 until n) {
      if (isZero(i, j) && marked(i)(j)== 2) return (i, j)
    }
    throw new RuntimeException(s"No primed zero found in row $i")
  }

  /**
    * Unstar the stars and star the primes in the path until the last element
    */
  def convert(path: Array[(Int, Int)], last : Int ) = {
    for (k <- 0 to last){
      val (i, j) = path(k)
      if (marked(i)(j) == 1) marked(i)(j) = 0
      else marked(i)(j) = 1
    }
  }

  /**
    * Uncover all rows/columns
    */
  def clear() = {
    coveredColumns = Array.fill[Boolean](n)(elem = false)
    coveredRows =  Array.fill[Boolean](n)(elem = false)
  }

  /**
    * Erase all primed
    */
  def erasePrime() = {
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        if (marked(i)(j) == 2) {
          marked(i)(j) = 0
        }
      }
    }
  }

  /**
    * Returns the minimal value which is not covered
    */
  def minimalUncorvedValue(): Double = {
    var min = Double.MaxValue
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        if (!coveredRows(i) && !coveredColumns(j) && cost(i)(j) < min) min = cost(i)(j)
      }
    }
    min
  }


  /**
    * Show costs
    */
  def showCost(): String = {
    var output =""
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        val strikethrough = (coveredColumns(j) || coveredRows(i)) match {
          case true => "- "
          case false => "  "
        }
        val masked = marked(i)(j) match {
          case 0 => " "
          case 1 => "*"
          case 2 => "'"
        }
        output += cost(i)(j) + masked + strikethrough
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
object MunkresSolver extends App {
  val debug = false
  import org.scamata.example.AAMAS4x4._
  println(pb)
  val solver = new MunkresSolver(pb, LF)
  val alloc =solver.run()
  println(alloc.toString)
  println(alloc.meanWorkload())

}
