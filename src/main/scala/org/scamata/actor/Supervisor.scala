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

  var solver : ActorRef = context.parent//Reference to the distributed solver
  var directory = new Directory()//White page for the peers
  var nbReady = 0//Number of agents which are ready to negotiate

  /**
    * Initially all the worker are active and the allocation is random
    */
  startWith(DefaultSupervisorState, new SupervisorStatus(Set[ActorRef](), Allocation.randomAllocation(pb)))


  /**
    * Method invoked after starting the actor
    */
  override def preStart(): Unit = {
    pb.workers.foreach{ worker : Worker => //For all workers
      val actor =  context.actorOf(Props(classOf[GiftBehaviour], worker, rule), worker.name)//Create the agent
      directory.add(worker, actor)//Add it to the directory
    }
  }

  /**
    * Message handling
    */
  when(DefaultSupervisorState) {
    //When the works should be done
    case Event(Start, status) =>
      solver = sender
      //Distribute the initial allocation
      directory.allActors().foreach{ actor: ActorRef =>
        val worker = directory.workers(actor)
        val bundle = status.allocation.bundle(worker)
        if (debug) println(s"Supervisor initiates $worker with bundle $bundle")
        actor ! Initiate(bundle, directory, pb.cost)
      }
      stay using status
    //When an actor becomes ready
    case Event(Ready, status) =>
      nbReady+=1
      if (nbReady == pb.m()){//When all of them are ready
        directory.allActors().foreach { actor: ActorRef => //Start them
          if (debug) println(s"Supervisor starts ${directory.workers(sender)}")
          actor ! Start
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
        solver ! Outcome(allocation)// reports the allocation
        directory.allActors().foreach(a => a ! Stop)// stops the actors
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