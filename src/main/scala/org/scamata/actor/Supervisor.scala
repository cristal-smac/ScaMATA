// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Allocation, MATA, Task, Worker}
import org.scamata.solver.SocialRule
import akka.actor.{Actor, ActorRef, FSM, Props}

import scala.collection.SortedSet

sealed trait SupervisorState
case object DefaultSupervisorState extends SupervisorState

/**
  * Supervisor which starts and stops the computation of a matching
  * @param pb MATA problem instance
  * @param rule to apply (Cmax or Flowtime)
  * */
class Supervisor(pb: MATA, rule: SocialRule) extends Actor with FSM[SupervisorState,Set[ActorRef]] {

  val debug=false

  var solver : ActorRef = context.parent
  var actors : Seq[ActorRef]= Seq[ActorRef]()//References to the workers
  var directory = new Directory()//White page for the workers

  val allocation : Allocation = Allocation.randomAllocation(pb)// Generate a random allocation

  /**
    * Initially all the worker are active
    */
  startWith(DefaultSupervisorState, Set[ActorRef]())


  /**
    * Method invoked after starting the actor
    */
  override def preStart(): Unit = {
    //Create the workers
    pb.workers.foreach{ worker : Worker =>
      val actor =  context.actorOf(Props(classOf[GiftBehaviour], worker, rule), worker.name)
      actors :+= actor
      directory.add(worker, actor)
    }
  }


  when(DefaultSupervisorState) {
    //When the works should be done
    case Event(Start, stoppedActor) =>
      solver = sender
      if (debug) println(s"Supervisor directory: $directory")
      //Initiate the beliefs, distribute the initial allocation and start the workers
      directory.allActors().foreach{ actor: ActorRef =>
        if (debug) println(s" Supervisor sends Initiate")
        actor ! Initiate(directory, pb.cost) // allocation.workloads()
        if (debug) println(s" Supervisor sends Give")
        actor ! Give(allocation.bundle(directory.workers(actor)))
      }
      stay using stoppedActor

    //When an actor becomes active
    case Event(ReStarted(bundle), alreadyStopped) =>
      val stoppedActor = alreadyStopped - sender
      allocation.bundle  =  allocation.bundle.updated(directory.workers(sender), bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} agent(s) in pause since ${directory.workers(sender)} leaves pause state with bundle $bundle")
      stay using stoppedActor


    // When an actor becomes inactive
    case Event(Stopped(bundle), alreadyStopped) =>
      val stoppedActor = alreadyStopped + sender
      allocation.bundle = allocation.bundle.updated(directory.workers(sender), bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} agent(s) in pause since ${directory.workers(sender)} enters in pause state with bundle $bundle")
      if (stoppedActor.size == pb.m()){// When all the actors are in pause
        solver ! Result(allocation)// reports the allocation
        actors.foreach(a => a ! Stop)// stops the actors
        context.stop(self)//stops the supervisor
      }
      stay using stoppedActor

    case Event(msg@_, stoppedActor) =>
      println("Supervisor receives a message which was not expected: "+msg)
      stay using stoppedActor
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}
