// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}
import org.scamata.actor._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import javax.naming.spi.DirStateFactory.Result

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
class DistributedGiftSolver(pb : MWTA, rule : SocialRule, system: ActorSystem) extends DealSolver(pb, rule) {

  val TIMEOUTVALUE : FiniteDuration = 120 minutes // Default timeout of a run
  implicit val timeout : Timeout = Timeout(TIMEOUTVALUE)
  // Launch a new solverAgent
  DistributedGiftSolver.id+=1
  val supervisor : ActorRef = system.actorOf(Props(classOf[SolverAgent], pb, rule), name = "solverAgent"+DistributedGiftSolver.id)

  /**
    * Returns an allocation modifying the initial one
    */
  def reallocate(allocation: Allocation): Allocation = {
    val future = supervisor ? Start(allocation)
    val result = Await.result(future, timeout.duration).asInstanceOf[Outcome]
    nbPropose = result.nbPropose
    nbAccept = result.nbAccept
    nbReject = result.nbReject
    nbWithdraw = result.nbWithdraw
    nbConfirm = result.nbConfirm
    nbInform = result.nbInform
    result.allocation
  }

}

object DistributedGiftSolver{
  var id = 0
  val debug = false
  def main(args: Array[String]): Unit = {
    import org.scamata.example.toy4x4._
    println(pb)
    var allocation = new Allocation(pb)
    allocation = allocation.update(a1, SortedSet(t4))
    allocation = allocation.update(a2, SortedSet(t3))
    allocation = allocation.update(a3, SortedSet(t1))
    allocation = allocation.update(a4, SortedSet(t2))
    println(allocation)
    val r = scala.util.Random
    val system = ActorSystem("DistributedGiftSolver" + r.nextInt.toString)
    //The Actor system
    val negotiationSolver = new DistributedGiftSolver(pb, LCmax, system)
    println("@startuml")
    println("skinparam monochrome true")
    println("hide footbox")
    println("participant SolverAgent")
    for (i<- 1 to pb.m) println(s"participant a$i")
    val sol = negotiationSolver.reallocate(allocation)
    println("@enduml")
    println(sol.toString)
    println(sol.makespan())
    System.exit(1)
  }
}