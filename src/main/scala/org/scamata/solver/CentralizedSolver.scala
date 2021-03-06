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
class CentralizedSolver(pb : MATA, rule : SocialRule, strategy : DealStrategy) extends DealSolver(pb, rule, strategy) {
  debug = false
  var trace = false

    /**
    * Reallocate the initial allocation
    */
  def reallocate(initialAllocation: Allocation): Allocation = {
    var a = initialAllocation
    var cons: ListBuffer[Worker] = Random.shuffle(pb.workers.to[ListBuffer])
    if (debug) println("All peers are initially potential contractors")
    while(cons.nonEmpty){
      cons.foreach { i: Worker =>
        var responders = pb.workers - i
        if (rule == LCmax) responders=responders.filter(j => a.workload(j) < a.workload(i))
        if (responders.isEmpty || a.bundle(i).isEmpty) {
          cons -= i
          if (debug) println(s"$i is desesperated")
        }
        else {
          var found = false
          var bestA: Allocation = a
          var bestD: Swap= new SingleSwap(i, NoWorker$, NoTask, NoTask)
          var bestGoal : Double = rule match {
            case LCmax => a.workload(i)
            case LF => 0.0
            case LC => 0.0
          }
          responders.foreach { r =>
            a.bundle(i).foreach { t1 =>
              val counterparts = strategy match {
                case SingleGiftOnly =>  Set[Task](NoTask)
                case SingleSwapAndSingleGift =>  a.bundle(r)+NoTask
                case SingleSwapOnly =>  a.bundle(r)
              }
              counterparts.foreach { t2 : Task =>
                val deal : Swap = t2 match {
                  case NoTask => new SingleGift(i, r, t1)
                  case _ => new SingleSwap(i, r, t1, t2)
                }
                val postA = a.apply(deal)
                val currentGoal : Double = rule match {
                  case LCmax =>
                    Math.max(postA.workload(i), postA.workload(r))
                  case LF =>
                    postA.delay(i) + postA.delay(r) - (a.delay(i) + a.delay(r))
                  case LC => pb.cost(r,t1) + pb.cost(i,t1) - (pb.cost(i,t1) + pb.cost(r,t2))

                }
                if (currentGoal < bestGoal) {
                  bestGoal = currentGoal
                  bestA = postA
                  bestD = deal
                  found = true
                }
              }
            }
          }
          // Select the best apply if any
          if (!found) {
            if (debug) println(s"$i is deseperated")
            cons -= i
          } else {
            if (debug || trace) println(s"$bestD")
            nbPropose += 1
            if (! bestD.isInstanceOf[SingleGift]) {
              nbCounterPropose += 1
              nbConfirmSwap +=1
            }
            else {
              nbPropose += 1
              nbConfirmGift += 1
            }
            nbAccept += 1
            a = bestA
            if (rule == LCmax) {
              cons = cons.toSet.union(pb.workers.filter(j => a.workload(j) > bestGoal)).to[ListBuffer]
            }
            cons = Random.shuffle(cons)
          }
        }
      }
    }
    a
  }
}

/**
  * Companion object to test it
  */
object CentralizedSolver extends App {
  val debug = false
  import org.scamata.example.Toy4x4._
  println(pb)
  val negotiationSolver = new CentralizedSolver(pb, LF, SingleSwapOnly)//SingleSwapAndSingleGift or SingleSwapOnly
  var allocation = new Allocation(pb)
  allocation = allocation.update(w1, SortedSet(t4))
  allocation = allocation.update(w2, SortedSet(t3))
  allocation = allocation.update(w3, SortedSet(t1))
  allocation = allocation.update(w4, SortedSet(t2))
  println(allocation)
  negotiationSolver.debug = true
  println(negotiationSolver.reallocate(allocation).toString)
}
