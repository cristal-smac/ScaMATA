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

  override def solve(): Allocation = {
    // 1 -- Reformulate the problem
    var startingTime: Long = System.nanoTime()
    var allocation = new Allocation(pb)
    // Built the matrix
    //      | cost(i)(j)  |
    //  Q = | 2cost(i)(j) |
    //      |    ...      |
    //      | ncost(i)(j) |
    val q=Array.ofDim[Double](pb.m*pb.n, pb.n)
    var i = 0
    for (k <- 1 to pb.n) {
      pb.workers.foreach{ case w=>
        var j = 0
        pb.tasks.foreach{ case t=>
          q(i)(j) = k * pb.cost(w,t)
          j+=1
        }
       i+=1
      }
    }
    if (debug) println("Q\n"+Matrix.show(q))

    //Translate it to a mincost max flow problem , i.e. a capacity matrix and a cost matrix
    val SIZE = pb.n*pb.m+pb.n+2 // of the network problem
    val capacity = Array.ofDim[Int](SIZE, SIZE)
    val cost = Array.ofDim[Double](SIZE, SIZE)
    for(i <- 0 until SIZE){
      for(j<- 0 until SIZE){
        if ( (i==0 && 2<=j && j < pb.m*pb.n+2) || (j==1 &&  pb.m*pb.n+2 <=i)){
          capacity(i)(j)=1 // link the source to the factories and the warehouses to the sink
        }
        val i2= i-2
        val j2 = j-(pb.m*pb.n+2)
        if (0<=i2 && i2< pb.n*pb.m && 0 <= j2){ // link the factories to the warehouses give the costs
          cost(i)(j)=q(i2)(j2)
          capacity(i)(j)=1
        }
      }
    }

    if (debug){
      println("Cost\n"+Matrix.show(cost))
      println("Capacity\n"+Matrix.show(capacity))
    }
    preSolvingTime = System.nanoTime() - startingTime

    //2 -- Solve the flow problem
    var flow = new MinCostMaxFlow(capacity, cost, source = 0, sink = 1)
    flow.getMaxFlow()

    //3 -- Translate flow into an allocation
    startingTime = System.nanoTime()
    var (istart,jstart)= (2, pb.m*pb.n+2)

    for (j<- 0 until pb.n) {
      for (i <- 0 until pb.m * pb.n) {
        if (debug) println(s"$i, $j")
        if (flow.flow(i+istart)(j+jstart) == 1) {
          val taskNumber = j
          val agentNumber = i%pb.m
          if (debug) println(s"$i, $j (found): ${taskNumber}, ${agentNumber}")
          val task: Task = pb.tasks.toVector(taskNumber)
          val agent = pb.workers.toVector(agentNumber)
          allocation.bundle += (agent -> (allocation.bundle(agent) + task))
        }
      }
    }

    if (debug) {
      println("Flow\n" + Matrix.show(flow.flow))
    }
    postSolvingTime = System.nanoTime() - startingTime
    if (debug) println("A\n"+allocation)
    allocation
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
