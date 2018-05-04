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
class DistributedGiftSolver(pb : MATA, rule : SocialRule, system: ActorSystem) extends DealSolver(pb, rule) {

  val TIMEOUTVALUE  = 1000 seconds // default timeout of a run
  implicit val timeout = Timeout(TIMEOUTVALUE)

  /**
    * Returns an allocation
    */
  override def solve(): Allocation = {
    // Launch a new supervisor
    DistributedGiftSolver.id+=1
    if (debug) system.eventStream.setLogLevel(akka.event.Logging.DebugLevel)
    val supervisor = system.actorOf(Props(classOf[Supervisor], pb, rule), name = "supervisor"+DistributedGiftSolver.id)
    // The current thread is blocked and it waits for the supervisor to "complete" the Future with it's reply.
    val future = supervisor ? Start
    val result = Await.result(future, timeout.duration).asInstanceOf[Outcome]
    nbDeal = result.nbDeal
    result.allocation
  }

}

object DistributedGiftSolver{
  var id = 0
  val debug = false
  def main(args: Array[String]): Unit = {
    import org.scamata.example.toy4x4._
    //import org.scamata.example.bug2x4
    //val pb = MATA.randomProblem(4, 22)
    println(pb)
    val r = scala.util.Random
    val system = ActorSystem("DistributedGiftSolver" + r.nextInt.toString)
    //The Actor system
    val negotiationSolver = new DistributedGiftSolver(pb, Flowtime, system)
    println(negotiationSolver.run().toString)
    System.exit(1)
  }
}