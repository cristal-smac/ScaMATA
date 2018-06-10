// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}

import org.scamata.actor._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.collection.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Distributed solver based on multi-agent negotiation process of single gift in order to minimize the rule
  * @param pb to be solver
  * @param rule to be optimized
  * @param system of Actors
  */
class DistributedSolver(pb : MWTA, rule : SocialRule, strategy : DealStrategy, system: ActorSystem) extends DealSolver(pb, rule, strategy) {

  val TIMEOUTVALUE : FiniteDuration = 600 minutes // Default timeout of a run
  implicit val timeout : Timeout = Timeout(TIMEOUTVALUE)
  // Launch a new solverAgent
  DistributedSolver.id+=1
  val supervisor : ActorRef = system.actorOf(Props(classOf[SolverAgent], pb, rule, strategy), name = "solverAgent"+DistributedSolver.id)

  /**
    * Returns an allocation modifying the initial one
    */
  def reallocate(allocation: Allocation): Allocation = {
    if (debug) println("@startuml")
    if (debug) println("skinparam monochrome true")
    if (debug) println("hide footbox")
    if (debug) {
      for (i<- 1 to pb.m) println(s"participant $i")
    }
    if (debug) supervisor ! Trace
    val future = supervisor ? Start(allocation)
    val result = Await.result(future, timeout.duration).asInstanceOf[Outcome]
    if (debug) println("@enduml")
    nbPropose = result.nbPropose
    nbCounterPropose = result.nbCounterPropose
    nbAccept = result.nbAccept
    nbReject = result.nbReject
    nbWithdraw = result.nbWithdraw
    nbConfirmGift = result.nbConfirmGift
    nbConfirmSwap = result.nbConfirmSwap
    nbInform = result.nbInform
    if (debug) println(result.allocation)
    result.allocation
  }

}

object DistributedSolver{
  var id = 0
  val debug = false
  def main(args: Array[String]): Unit = {
    import org.scamata.example.toy4x4._
    println(pb)
    var allocation = new Allocation(pb)
    allocation = allocation.update(w1, SortedSet(t4))
    allocation = allocation.update(w2, SortedSet(t3))
    allocation = allocation.update(w3, SortedSet(t1))
    allocation = allocation.update(w4, SortedSet(t2))
    println(allocation)
    val r = scala.util.Random
    val system = ActorSystem("DistributedSolver" + r.nextInt.toString)
    //The Actor system
    val negotiationSolver = new DistributedSolver(pb, LCmax, SingleSwapAndSingleGift , system)// SingleGiftOnly
    negotiationSolver.debug = true
    val sol = negotiationSolver.reallocate(allocation)
    println(sol.toString)
    println("Makespan "+sol.makespan())
    println("NbCounterProposal "+negotiationSolver.nbCounterPropose)
    System.exit(1)
  }
}