// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Allocation, MWTA, Worker}
import org.scamata.solver.{DealStrategy, SingleGiftOnly, SingleSwapAndSingleGift, SocialRule}
import akka.actor.{Actor, ActorRef, FSM, Props}

// The solverAgent behaviour is described by a single state FSM
sealed trait SolverState
case object DefaultSolverState extends SolverState

/**
  * Immutable state of the solverAgent
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
  * SolverAgent which starts and stops the computation of an allocation
  * @param pb MWTA problem instance
  * @param rule to apply (LCmax or LC)
  * @param strategy for selecting the kind of deal
  * */
class SolverAgent(pb: MWTA, rule: SocialRule, strategy : DealStrategy) extends Actor with FSM[SolverState,SupervisorStatus] {

  var debug = false

  var solver : ActorRef = context.parent // Reference to the distributed solver
  var directory = new Directory() // White page for the peers
  var nbReady = 0 // Number of workers which are ready to negotiate
  var finishedActor : Set[ActorRef] = Set[ActorRef]() // Number of workers which are deseperated
  var (nbPropose, nbCounterPropose, nbAccept, nbReject, nbWithdraw, nbConfirmGift, nbConfirmSwap, nbCancel, nbInform) = (0, 0, 0, 0, 0, 0, 0, 0, 0)

  /**
    * Initially all the worker are active and the allocation is empty
    */
  startWith(DefaultSolverState, new SupervisorStatus(Set[ActorRef](), new Allocation(pb)))

  /**
    * Method invoked after starting the actor
    */
  override def preStart(): Unit = {
    pb.workers.foreach{ worker : Worker => //For all workers
      val actor = context.actorOf(Props(classOf[WorkerAgentBehaviour], worker, rule, strategy), worker.name)
      directory.add(worker, actor) // Add it to the directory
    }
  }

  /**
    * Message handling
    */
  when(DefaultSolverState) {
    //In order to debug
    case Event(Trace, status) =>
      directory.allActors().foreach( _ ! Trace)
      stay using status

    //When the works should be done
    case Event(Start(allocation), status) =>
      solver = sender
      //Distribute the initial allocation
      directory.allActors().foreach { actor: ActorRef =>
        val worker = directory.workers(actor)
        val bundle = allocation.bundle(worker)
        if (debug) println(s"SolverAgent initiates $worker with bundle $bundle")
        actor ! Initiate(bundle, directory, pb.costMatrix)
      }
      stay using new SupervisorStatus(status.stoppedActors, allocation)

    //When an actor becomes ready
    case Event(Ready, status) =>
      nbReady += 1
      if (nbReady == pb.m()) {
        //When all of them are ready
        directory.allActors().foreach { actor: ActorRef => //Trigger them
          if (debug) println(s"SolverAgent starts ${directory.workers(actor)}")
          actor ! Trigger
        }
      }
      stay using status

    //When an actor becomes active
    case Event(Activated(bundle), status) =>
      val worker = directory.workers(sender)
      val stoppedActor = status.stoppedActors - sender
      val allocation = status.allocation.update(worker, bundle)
      if (debug) println(s"SolverAgent: ${stoppedActor.size} agent(s) in pause since $worker restarts with bundle $bundle")
      stay using new SupervisorStatus(stoppedActor, allocation)

    // When an actor becomes inactive
    case Event(Stopped(bundle), status) =>
      val worker = directory.workers(sender)
      val stoppedActor = status.stoppedActors + sender
      val allocation = status.allocation.update(worker, bundle)
      if (debug) println(s"SolverAgent: ${stoppedActor.size} agent(s) in pause since $worker stops with bundle $bundle")
      if (stoppedActor.size == pb.m()) {
        if (debug) println(s"SolverAgent: all the actors are in pause")
        directory.allActors().foreach(a => a ! Query) // stops the actors
      }
      stay using new SupervisorStatus(stoppedActor, allocation)

    case Event(Finish(nbP, nbCP, nbA, nbR, nbW, nbConfG, nbConfS, nbCan, nbI), status) =>
      if (!finishedActor.contains(sender)) {
        nbPropose += nbP
        nbCounterPropose += nbCP
        nbAccept += nbA
        nbReject += nbR
        nbWithdraw += nbW
        nbConfirmGift += nbConfG
        nbConfirmSwap += nbConfS
        nbCancel += nbCan
        nbInform += nbI
        finishedActor += sender
      }
      if (finishedActor.size  == pb.m() && status.stoppedActors.size  == pb.m()) {
        solver ! Outcome(status.allocation, nbPropose, nbCounterPropose, nbAccept, nbReject, nbWithdraw, nbConfirmGift, nbConfirmSwap, nbCancel, nbInform) // reports the allocation
        directory.allActors().foreach(a => a ! Stop) // stops the actors
        context.stop(self) //stops the solverAgent
      }
      stay using status

    case Event(msg@_, status) =>
      println("SolverAgent receives a message which was not expected: " + msg)
      stay using status
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}