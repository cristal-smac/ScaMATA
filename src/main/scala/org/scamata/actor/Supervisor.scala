// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Allocation, MATA, Worker}
import org.scamata.solver.SocialRule

import akka.actor.{Actor, ActorRef, FSM, Props}

// The supervisor behaviour is described by a single state FSM
sealed trait SupervisorState
case object DefaultSupervisorState extends SupervisorState

/**
  * Immutable state of the supervisor
  * @param stoppedActors
  * @param allocation
  */
class SupervisorStatus(val stoppedActors: Set[ActorRef], val allocation: Allocation) extends
  Product2[Set[ActorRef], Allocation]{
  override def _1: Set[ActorRef] = stoppedActors
  override def _2: Allocation = allocation
  override def canEqual(that: Any): Boolean = that.isInstanceOf[SupervisorStatus]
}

/**
  * Supervisor which starts and stops the computation of an allocation
  * @param pb MATA problem instance
  * @param rule to apply (Cmax or Flowtime)
  * */
class Supervisor(pb: MATA, rule: SocialRule) extends Actor with FSM[SupervisorState,SupervisorStatus] {

  val debug = false
  val extraDebug = false

  var solver : ActorRef = context.parent
  var actors : Seq[ActorRef]= Seq[ActorRef]()//References to the workers
  var directory = new Directory()//White page for the workers
  var nbReady = 0

  /**
    * Initially all the worker are active and the allocation is random
    */
  startWith(DefaultSupervisorState, new SupervisorStatus(Set[ActorRef](), Allocation.randomAllocation(pb)))


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

  /**
    * Message handling
    */
  when(DefaultSupervisorState) {
    //When the works should be done
    case Event(Start, status) =>
      solver = sender
      if (extraDebug) println(s"Supervisor directory: $directory")
      //Initiate the beliefs, distribute the initial allocation and start the workers
      directory.allActors().foreach{ actor: ActorRef =>
        val worker = directory.workers(actor)
        if (debug) println(s"Supervisor initiates $worker")
        actor ! Initiate(directory, pb.cost)
      }
      stay using status

    //When an actor becomes ready
    case Event(Ready, status) =>
      nbReady+=1
      if (nbReady == pb.m()){
        directory.allActors().foreach { actor: ActorRef =>
          val worker = directory.workers(actor)
          val bundle = status.allocation.bundle(worker)
          if (debug) println(s"Supervisor gives $bundle to $worker")
          actor ! Give(bundle)
        }
      }
      stay using status

    //When an actor becomes active
    case Event(ReStarted(bundle), status) =>
      val worker = directory.workers(sender)
      val stoppedActor = status.stoppedActors - sender
      val allocation =  status.allocation.update(worker, bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} agent(s) in pause since $worker restarts with bundle $bundle")
      stay using new SupervisorStatus(stoppedActor, allocation)


    // When an actor becomes inactive
    case Event(Stopped(bundle), status) =>
      val worker = directory.workers(sender)
      val stoppedActor = status.stoppedActors + sender
      val allocation = status.allocation.update(worker, bundle)
      if (debug) println(s"Supervisor: ${stoppedActor.size} agent(s) in pause since $worker stops with bundle $bundle")
      if (stoppedActor.size == pb.m()){// When all the actors are in pause
        solver ! Result(allocation)// reports the allocation
        actors.foreach(a => a ! Stop)// stops the actors
        context.stop(self)//stops the supervisor
      }
      stay using new SupervisorStatus(stoppedActor, allocation)

    case Event(msg@_, status) =>
      println("Supervisor receives a message which was not expected: "+msg)
      stay using status
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}