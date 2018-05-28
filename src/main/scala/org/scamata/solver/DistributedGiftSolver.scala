// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MWTA}
import org.scamata.actor._
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import javax.naming.spi.DirStateFactory.Result

import scala.collection.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Distributed multiagent negotiation process for minimizing the rule
  *
  * @param pb to be solver
  * @param rule to be optimized
  * @param system of Actors
  */
class DistributedGiftSolver(pb : MWTA, rule : SocialRule, system: ActorSystem) extends DealSolver(pb, rule) {

  val TIMEOUTVALUE  = 1000000 seconds // default timeout of a run
  implicit val timeout = Timeout(TIMEOUTVALUE)

  val supervisor = system.actorOf(Props(classOf[Supervisor], pb, rule), name = "supervisor"+DistributedGiftSolver.id)

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
   reallocate(Allocation.randomAllocation(pb))
  }
  /**
    * Returns an allocation
    */
  def reallocate(allocation: Allocation): Allocation = {
    // Launch a new supervisor
    DistributedGiftSolver.id+=1
    if (debug) system.eventStream.setLogLevel(akka.event.Logging.DebugLevel)
    //val supervisor = system.actorOf(Props(classOf[Supervisor], pb, rule, allocation), name = "supervisor"+DistributedGiftSolver.id)
    // The current thread is blocked and it waits for the supervisor to "complete" the Future with it's reply.
    val future = supervisor ? Trigger(allocation)
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
    val negotiationSolver = new DistributedGiftSolver(pb, Cmax, system)
    println("@startuml")
    println("skinparam monochrome true")
    println("hide footbox")
    println("participant Supervisor")
    for (i<- 1 to pb.m) println(s"entity a$i")
    val sol = negotiationSolver.reallocate(allocation)
    println("@enduml")
    println(sol.toString)
    println(sol.makespan())
    System.exit(1)
  }
}