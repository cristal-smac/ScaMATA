// Copyright (C) Maxime MORGE 2018
package org.scamata.solver

import org.scamata.core.{Allocation, MATA}

import org.scamata.actor._

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
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
class DistributedGiftSolver(pb : MATA, rule : SocialRule, system: ActorSystem) extends Solver(pb, rule) {

  if (rule == Flowtime) throw new RuntimeException("DistributedGiftSolver does not support yet flowtime")
  val TIMEOUTVALUE = 100 seconds
  // default timeout of a run
  implicit val timeout = Timeout(TIMEOUTVALUE)

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    // Launch a new supervisor
    DistributedGiftSolver.id+=1
    if (debug) system.eventStream.setLogLevel(akka.event.Logging.DebugLevel)
    val supervisor = system.actorOf(Props(classOf[GiftSupervisor], pb, rule), name = "supervisor"+DistributedGiftSolver.id)
    // The current thread is blocked and it waits for the supervisor to "complete" the Future with it's reply.
    val future = supervisor ? Trigger
    val result = Await.result(future, timeout.duration).asInstanceOf[Result]
    result.allocation
  }

}

object DistributedGiftSolver{
  var id = 0
}