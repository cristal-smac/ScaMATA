// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.core.{NoTask,Task}
import org.scamata.deal.{Deal, Swap, SingleSwap, SingleGift}

import scala.collection.SortedSet
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Minimizing the rule by applying single gift
  * @param pb to be solver
  * @param rule to be optimized
  * @param strategy for selecting the kind of deal
  */
class CentralizedSolver(pb : MWTA, rule : SocialRule, strategy : DealStrategy) extends DealSolver(pb, rule, strategy) {
  debug = false
  val trace = false

    /**
    * Reallocate the initial allocation
    */
  def reallocate(initialAllocation: Allocation): Allocation = {
    var allocation = initialAllocation
    var contractors: ListBuffer[Worker] = Random.shuffle(pb.workers.to[ListBuffer])
    if (debug) println("All peers are initially potential contractors")
    while(contractors.nonEmpty){
      contractors.foreach { initiator: Worker =>
        if (debug) println(s"$initiator tries to find a single gift which is socially rational")
        var responders = pb.workers - initiator
        if (rule == LCmax) responders=responders.filter(j => allocation.workload(j) < allocation.workload(initiator))
        if (responders.isEmpty || allocation.bundle(initiator).isEmpty) {
          contractors -= initiator
          if (debug) println(s"$initiator is desesperated")
        }
        else {
          var found = false
          var bestAllocation: Allocation = allocation
          var bestSingleSwap: Swap = new SingleSwap(initiator, NoWorker, NoTask, NoTask)
          var bestGoal = rule match {
            case LCmax => allocation.workload(initiator)
            case LC => 0.0
          }
          responders.foreach { responder =>
            allocation.bundle(initiator).foreach { task1 =>
              val counterparts = strategy match {
                case SingleGiftOnly =>  Set[Task](NoTask)
                case SingleSwapAndSingleGift =>  allocation.bundle(responder)+NoTask
              }
              counterparts.foreach { task2 : Task =>
                val deal : Swap = task2 match {
                  case NoTask => new SingleGift(initiator, responder, task1)
                  case _ => new SingleSwap(initiator, responder, task1, task2)
                }
                val postAllocation = allocation.apply(deal)
                val currentGoal = rule match {
                  case LCmax =>
                    Math.max(postAllocation.workload(initiator), postAllocation.workload(responder))
                  case LC =>
                    pb.cost(responder, task1) - pb.cost(initiator, task1) + pb.cost(initiator, task2) - pb.cost(responder, task2)
                }
                if (currentGoal < bestGoal) {
                  bestGoal = currentGoal
                  bestAllocation = postAllocation
                  bestSingleSwap = deal
                  found = true
                }
              }
            }
          }
          // Select the best apply if any
          if (!found) {
            if (debug) println(s"$initiator is deseperated")
            contractors -= initiator
          } else {
            if (debug || trace) println(s"$bestSingleSwap")
            nbPropose += 1
            nbAccept += 1
            nbConfirm += 1
            allocation = bestAllocation
            if (rule == LCmax) {
              contractors = contractors.toSet.union(pb.workers.filter(j => allocation.workload(j) > bestGoal)).to[ListBuffer]
            }
            contractors = Random.shuffle(contractors)

          }
        }
      }
    }
    allocation
  }
}

/**
  * Companion object to test it
  */
object CentralizedSolver extends App {
  val debug = false
  import org.scamata.example.toy4x4._
  println(pb)
  val negotiationSolver = new CentralizedSolver(pb,LCmax,SingleSwapAndSingleGift)
  var allocation = new Allocation(pb)
  allocation = allocation.update(a1, SortedSet(t4))
  allocation = allocation.update(a2, SortedSet(t3))
  allocation = allocation.update(a3, SortedSet(t1))
  allocation = allocation.update(a4, SortedSet(t2))
  println(allocation)
  println(negotiationSolver.reallocate(allocation).toString)
}
