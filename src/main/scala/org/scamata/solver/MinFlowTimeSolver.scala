// Copyright (C) Maxime MORGE 2018
package org.scamata.solver
import org.scamata.core.Allocation.debug
import org.scamata.core.{Allocation, MATA, Task}
import org.scamata.util.{Matrix, MinCostMaxFlow, TransportationWriter}

import scala.sys.process._

/**
  * Algorithm from "Scheduling independent tasks to reduce mean finishing time" by
  * Bruno, James and Coffman Jr, Edward G and Sethi, Ravi using Ford-Ferguson algorithm
  * @param pb to be solver
  * @param rule to be optimized
  */
class MinFlowTimeSolver(pb : MATA, rule : SocialRule) extends DualSolver(pb, rule) {
  debug = false
  if (rule != LF) throw new RuntimeException("MinFlowTimeSolver only works with the social rule LF")

  var allocation = new Allocation(pb)
  val N = pb.n*pb.m+pb.n+2 // The implicit number of vertex in the network problem
  val source: Int = 0
  val sink: Int = 1
  var found: Array[Boolean] = new Array[Boolean](N)
  var flow = Array.ofDim[Int](N, N)
  var dist = new Array[Double](N + 1)
  var dad = new Array[Int](N)
  var pi = new Array[Double](N)
  var totalFlow = 0
  var totalCost = 0.0

  /** The matrix
  *      | cost(i)(j)  |
  *  Q = | 2cost(i)(j) |
  *      |    ...      |
  *      | ncost(i)(j) |
    *      */
  def q(i: Integer, j: Integer): Double = {
    val k: Integer = i / pb.m +1
    val i2: Integer = i % pb.m
    val task: Task = pb.tasks.toVector(j)
    val worker = pb.workers.toVector(i2)
    k * pb.cost(worker,task)
  }

  /** The capacity of the edge between the nodes
    * @param i the source node
    * @param j the target node
    */
  def capacity(i: Integer, j: Integer) : Integer = {
    // link the source to the factories and the warehouses to the sink
    if ( (i==0 && 2<=j && j < pb.m*pb.n+2) || (j==1 &&  pb.m*pb.n+2 <=i)) return  1
    // link the factories to the warehouses
    val i2= i-2
    val j2 = j-(pb.m*pb.n+2)
    if (0<=i2 && i2< pb.n*pb.m && 0 <= j2) return 1
    return 0
  }

  /** The cost of the edge between the nodes
    * @param i the source node
    * @param j the target node
    */
  def cost(i: Integer, j: Integer): Double = {
    val i2= i-2
    val j2 = j-(pb.m*pb.n+2)
    if (0<=i2 && i2< pb.n*pb.m && 0 <= j2) return q(i2,j2)
    return 0.0
  }

  /**
    * Solver main method
    * @return the allocation
    */
  override def solve(): Allocation = {
    // 1 -- Reformulate the problem
    var startingTime: Long = System.nanoTime()
    preSolvingTime = System.nanoTime() - startingTime

    if (debug){
      println("Q\n"+Matrix.show(q,pb.n*pb.m,pb.n))
      println("Cost\n"+Matrix.show(cost,N,N))
      println("Capacity\n"+Matrix.show(capacity,N,N))
    }

    //2 -- Solve the flow problem
    if (debug) println("Solve the flow problem")
    getMaxFlow()

    //3 -- Translate flow into an allocation
    if (debug) println("Translate flow into an allocation")
    startingTime = System.nanoTime()
    if (debug) {
      println("Flow\n" + Matrix.show(flow))
    }
    translateFlow()
    postSolvingTime = System.nanoTime() - startingTime
    if (debug) println("A\n"+allocation)
    allocation
  }

  /**
    * Returns true if there is a path from source 's' to sink 't' in
    * residual graph. Also fills ?[] to store the path
    */
  def search(source: Int, sink: Int): Boolean = {
    var src = source
    found = Array.fill(N)(false)
    dist = Array.fill(N+1)(Integer.MAX_VALUE)
    dist(src) = 0
    while (src != N) {
      var best = N
      found(src) = true
      for (k <- 0 until N) {
        //if (found[k]) continue
        if (!found(k)) {
          if (flow(k)(src) != 0) {
            val currentValue = dist(src) + pi(src) - pi(k) - cost(k,src)
            if (dist(k) > currentValue){
              dist(k) = currentValue
              dad(k) = src
            }
          }
          if (flow(src)(k) < capacity(src,k)){
            val currentValue = dist(src) + pi(src) - pi(k) + cost(src,k)
            if (dist(k) > currentValue){
              dist(k) = currentValue
              dad(k) = src
            }
          }
          if (dist(k) < dist(best)) best = k
        }
      }
      src = best
    }
    for (k <- 0 until N) {
      pi(k) = Math.min(pi(k) + dist(k), Double.MaxValue)
    }
    found(sink)
  }

  /**
    * Execute the minimal-cost maximal flow algorithm and returns
    * @return totalCost, totalFlow
    */
  def getMaxFlow() : (Int, Double) = {
    while (search(source,sink)){
      var residualCapacity =Integer.MAX_VALUE
      var x = sink
      while(x != source){
        residualCapacity = Math.min(residualCapacity, if (flow(x)(dad(x)) != 0) flow(x)(dad(x))
        else capacity(dad(x),x) - flow(dad(x))(x))
        x = dad(x)
      }
      x = sink
      while (x != source) {
        if (flow(x)(dad(x)) != 0) {
          flow(x)(dad(x)) -= residualCapacity
          totalCost -= residualCapacity * cost(x,dad(x))
        } else {
          flow(dad(x))(x) += residualCapacity
          totalCost += residualCapacity * cost(dad(x),x)
        }
        x = dad(x)
      }
      totalFlow += residualCapacity
    }
    (totalFlow, totalCost)
  }

  /**
    * Translate the flow in an allocation
    */
  def translateFlow(): Unit ={
    var (istart,jstart)= (2, pb.m*pb.n+2)
    for (j<- 0 until pb.n) {
      for (i <- 0 until pb.m * pb.n) {
        if (debug) println(s"$i, $j")
        if (flow(i+istart)(j+jstart) == 1) {
          val taskNumber = j
          val agentNumber = i%pb.m
          if (debug) println(s"$i, $j (found): ${taskNumber}, ${agentNumber}")
          val task: Task = pb.tasks.toVector(taskNumber)
          val agent = pb.workers.toVector(agentNumber)
          allocation.bundle += (agent -> (allocation.bundle(agent) + task))
        }
      }
    }
  }
}


/**
  * Companion object to test it
  */
object MinFlowTimeSolver extends App {
  val debug = true
  import org.scamata.example.Toy4x2._
  if (debug) println(pb)
  val lpSolver = new MinFlowTimeSolver(pb,LF)
  lpSolver.debug = true
  println(lpSolver.run().toString)
}
