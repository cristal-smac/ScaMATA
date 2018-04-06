// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{MATA, Allocation, Worker}
import org.scamata.solver.SocialRule

import akka.actor.{Actor, ActorRef, Props}


/**
  * Supervisor which starts and stop the computation of a matching
  * @param pb MATA problem instance
  * @param rule to apply (Cmax or Flowtime)
  * */
class Supervisor(pb: MATA, rule: SocialRule) extends Actor {

  val debug=false

  var solver : ActorRef = context.parent
  var actors : Seq[ActorRef]= Seq[ActorRef]()//References to the agents
  var directory = new Directory()//White page for the agents

  val allocation : Allocation = Allocation.randomAllocation(pb)// Generate a random allocation
  var stoppedActor : Set[ActorRef] = Set[ActorRef]()

  /**
    * Method invoked after starting the actor
    */
  override def preStart(): Unit = {
    //Create the agents
    pb.agents.foreach{ worker : Worker =>
      val actor =  context.actorOf(Props(classOf[Behaviour], worker, rule), worker.name)
      actors :+= actor
      directory.add(worker, actor)
    }
  }

  /**
    * Method invoked when a message is received
    */
  def receive : PartialFunction[Any, Unit] = {
    //When the works should be done
    case Start =>
      solver = sender
      if (debug) println(s"Supervisor directory: $directory")
      //Initiate the beliefs, distribute the initial allocation and start the agents
      directory.allActors().foreach{ case actor: ActorRef =>
        if (debug) println(s" Supervisor sends Initiate")
        actor ! Initiate(directory, pb.cost)
        if (debug) println(s" Supervisor sends Give")
        actor ! Give(allocation.bundle(directory.worker(actor)))
      }

    //When an actor becomes active
    case ReStarted(bundle) =>
      stoppedActor -= sender
      allocation.bundle += (directory.worker(sender) -> bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} paused agents since ${directory.worker(sender)} leaves pause state with bundle $bundle")

    // When an actor becomes inactive
    case Stopped(bundle) =>
      stoppedActor += sender
      allocation.bundle += (directory.worker(sender) -> bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} paused agents since ${directory.worker(sender)} is in pause state with bundle $bundle")
      if (stoppedActor.size == pb.m()){// When all the actors are in pause
        solver ! Result(allocation)// reports the allocation
        actors.foreach(a => a ! Stop)// stops the actors
        context.stop(self)//stops the supervisor
      }

    case msg@_ => println("Supervisor receives a message which was not expected: "+msg)
  }
}
