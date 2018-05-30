// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core._
import org.scamata.deal.SingleGift

import scala.collection.SortedSet
import scala.util.Random
import scala.collection.mutable.ListBuffer

/**
  * Minimizing the rule by applying single gift
  * @param pb to be solver
  * @param rule to be optimized
  */
class GiftSolver(pb : MWTA, rule : SocialRule) extends DealSolver(pb, rule) {
  debug = false
  val trace = false

    /**
    * Reallocate
    */
  def reallocate(initialAllocation: Allocation): Allocation = {
    var allocation = initialAllocation
    var contractors: ListBuffer[Worker] = Random.shuffle(pb.workers.to[ListBuffer])
    if (debug) println("All peers are initially potential contractors")

    while(contractors.nonEmpty){
      contractors.foreach { initiator: Worker =>
        if (debug) println(s"$initiator tries to find a single gift which is socially rational")
        val potentialPartners = rule match {
          case LC => // all the peers
            pb.workers - initiator
          case LCmax => // the peers with a smallest workload
            (pb.workers - initiator).filter(allocation.workload(_) < allocation.workload(initiator))
        }
        if (debug) println(s"Potential partner: $potentialPartners")
        if (potentialPartners.isEmpty || allocation.bundle(initiator).isEmpty) {
          contractors -= initiator
          if (debug) println(s"$initiator becomes inactive")
        }
        else {
          var found = false
          var bestAllocation: Allocation = allocation
          var bestSingleGift: SingleGift = new SingleGift(initiator, initiator, NoTask)
          var bestGoal = rule match {
            case LCmax => allocation.workload(initiator)
            case LC => 0.0
          }
          potentialPartners.foreach { supplier =>
            allocation.bundle(initiator).foreach { task =>
              // 4 - Foreach potential apply
              val gift = new SingleGift(initiator, supplier, task)
              val modifiedAllocation = allocation.apply(gift)
              val currentGoal = rule match { // Compute the new goal
              case LCmax =>
                Math.max(modifiedAllocation.workload(initiator), modifiedAllocation.workload(supplier))
              case LC =>
                pb.cost(supplier, task) - pb.cost(initiator, task)
            }
              if (currentGoal < bestGoal) {
                bestGoal = currentGoal
                bestAllocation = modifiedAllocation
                bestSingleGift = gift
                found = true
              }
            }
          }
          // Select the best apply if any
          if (!found) {
            if (debug) println(s"$initiator becomes inactive")
            contractors -= initiator
          } else {
            if (debug || trace) println(s"$bestSingleGift")
            nbPropose += 1
            nbAccept += 1
            nbConfirm += 1
            allocation = bestAllocation
/*
            if (rule == LCmax && ! contractors.contains(bestSingleGift.supplier)) {
              contractors = bestSingleGift.supplier :: contractors
            }
*/
            if (rule == LCmax) {
              pb.workers.filter(worker => allocation.workload(worker) > bestGoal &&  !contractors.contains(worker)).foreach { worker =>
                contractors += worker
              }
              contractors = Random.shuffle(contractors)
            }
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
object GiftSolver extends App {
  val debug = false
  import org.scamata.example.toy4x4._
  println(pb)
  val negotiationSolver = new GiftSolver(pb,LCmax)
  var allocation = new Allocation(pb)
  allocation = allocation.update(a1, SortedSet(t4))
  allocation = allocation.update(a2, SortedSet(t3))
  allocation = allocation.update(a3, SortedSet(t1))
  allocation = allocation.update(a4, SortedSet(t2))
  println(allocation)
  println(negotiationSolver.reallocate(allocation).toString)
}