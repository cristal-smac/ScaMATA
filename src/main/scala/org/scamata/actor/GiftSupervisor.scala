// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.{Actor, ActorRef, Props}
import org.scamata.core
import org.scamata.core._
import org.scamata.solver.SocialRule

/**
  * Supervisor which starts and stop the computation of a matching
  * @param pb MATA problem instance
  * @param rule to apply (Cmax or Flowtime)
  * */
class GiftSupervisor(pb: MATA, rule: SocialRule) extends Actor {

  val debug=false

  var solver : ActorRef = _
  var actors = Seq[ActorRef]()//References to the agents
  var directory = new Directory()//White page for the agents

  val allocation = Allocation.randomAllocation(pb)// Generate a random allocation
  var nbPause = 0

  /**
    * Method invoked after starting the actor
    */
  override def preStart(): Unit = {
    //Create the agents
    pb.agents.foreach{ case agent : Worker =>
      val actor =  context.actorOf(Props(classOf[Behaviour], agent, rule), agent.name)
      actors :+= actor
      directory.add(agent, actor)
    }
  }

  /**
    * Method invoked when a message is received
    */
  def receive = {
    //When the works should be done
    case Trigger => {
      solver = sender
      //Initiate the beliefs, distribute the initial allocation and start the agents
      directory.allActors().foreach{ case actor: Actor =>
        actor ! Initiate(directory, pb.cost)
        actor !  Give(allocation.bundle(directory.name(actor)))
      }
    }
    //When an worker becomes active
    case ReStarted(bundle) => {
      nbPause -= 1
      allocation.bundle += (directory.name(sender) -> bundle)
      if (debug) println(s"Supervisor: $nbPause paused agents since ${directory.name(sender)} leaves pause state with bundle $bundle")
    }
    // When an worker becomes inactive
    case Stopped(bundle) => {
      nbPause += 1
      allocation.bundle += (directory.name(sender) -> bundle)
      if (debug) println(s"Supervisor: $nbPause paused agents since ${directory.name(sender)} is in pause state with bundle $bundle")
      //If allActors the individual are assigned the build the matching
      if (nbPause == pb.m()){
        solver ! Result(allocation)// report the allocation
        actors.foreach(a => a ! Stop)
        context.stop(self)//stop the supervisor
      }
    }
    case msg@_ => println("Supervisor receives a message which was not expected: "+msg)
  }

}
