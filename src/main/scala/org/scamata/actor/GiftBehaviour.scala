// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker, NoWorker, NoTask}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import akka.actor.{FSM, Stash}
import scala.language.postfixOps
import scala.collection.SortedSet

/**
  * Gift negotiation behaviour
  * @param worker which is embedded
  * @param rule to optimize
  */
class GiftBehaviour(worker: Worker, rule: SocialRule) extends WorkerAgent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] with Stash{

  /**
    * Initially the worker is in the initial state with no bundle, no beliefs about the workloads and no task/opponent taken into consideration
    */
  startWith(Initial, new StateOfMind(SortedSet[Task](), Map[Worker, Double](), NoWorker, NoTask))

  /**
    * Either the worker is in Initial state
    */
  when(Initial) {
    // If the worker agent is initiated
    case Event(Initiate(bundle, d, c), mind) => // Initiate the directory and the cost matrix
      this.cost = c
      this.directory = d
      this.solverAgent = sender
      var updatedMind = mind.initBelief(bundle, directory.allWorkers())
      updatedMind = updatedMind.addBundle(bundle)
      val workload = worker.workload(bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      sender ! Ready
      stay using updatedMind

    // If the worker agent is triggered
    case Event(Trigger, mind) =>
      var updatedMind = mind
      val workload = updatedMind.belief(worker)
      if (sender == solverAgent) {// If the agent is triggered by the solverAgent
        broadcastInform(workload)
      }
      // Otherwise the mind is up to date
      val potentialPartners = rule match { // The potential partners are
        case Flowtime => // all the peers
          directory.peers(worker)
        case Cmax => // the peers with a smallest workload
          directory.peers(worker).filter(updatedMind.belief(_) < workload)
      }
      // Either the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty) {
        solverAgent ! Stopped(updatedMind.bundle)
        stay using updatedMind
      } else { // Otherwise
        var found = false
        var bestOpponent = worker
        var bestTask : Task =  NoTask
        var bestGoal = rule match {
          case Cmax =>
            workload
          case Flowtime =>
            0.0
        }
        potentialPartners.foreach { opponent =>
          updatedMind.bundle.foreach { task => // foreach potential single apply
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = updatedMind.belief(opponent) + cost(opponent, task)
            val giftGoal = rule match {
              case Cmax =>
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime =>
                cost(opponent, task) - cost(worker, task)
            }
            if (giftGoal < bestGoal) {
              found = true
              bestGoal = giftGoal
              bestOpponent = opponent
              bestTask = task
            }
          }
        }
        if (! found) {
          solverAgent ! Stopped(updatedMind.bundle)
          stay using updatedMind
        } else {
          solverAgent ! Activated(updatedMind.bundle)
          val opponent = directory.adr(bestOpponent)
          updatedMind = updatedMind.changeDelegation(bestOpponent, bestTask)
          if (trace) println(s"$worker -> $bestOpponent : Propose($bestTask)")
          opponent ! Propose(bestTask, workload)
          nbPropose+=1
          goto(Proposer) using updatedMind
        }
      }

    // If the worker agent receives a proposal
    case Event(Propose(task, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      if (acceptable(task, provider = opponent, supplier = worker, updatedMind)) {
        solverAgent ! Activated(updatedMind.bundle)
        if (trace) println(s"$worker -> $opponent : Accept($task)")
        sender ! Accept(task, updatedMind.belief(worker))
        nbAccept+=1
        goto(Responder) using updatedMind
      }else{
        val workload = updatedMind.belief(worker)
        if (trace) println(s"$worker -> $opponent : Reject($task)")
        sender ! Reject(task, workload)
        nbReject+=1
        goto(Initial) using updatedMind
      }

    // If the worker agent receives an acceptance
    case Event(Accept(task, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      val workload = worker.workload(updatedMind.bundle, cost)
      if (trace) println(s"$worker -> $opponent : Withdraw($task)")
      sender ! Withdraw(task, workload)
      nbWithdraw += 1
      stay using updatedMind

    // If the worker agent receives a rejection
    case Event(Reject(_, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      stay using updatedMind

    // If the worker agent receives a withdrawal
    case Event(Withdraw(_, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      stay using updatedMind

    // If the worker agent receives a confirmation
    case Event(Confirm(task, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val workload = worker.workload(mind.bundle, cost)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      if (trace) println(s"$worker -> $opponent : Cancel($task)")
      sender ! Cancel(task, workload)
      nbCancel += 1
      stay using updatedMind

    // If the worker agent receives a cancel
    case Event(Cancel(task, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val bundle = mind.bundle + task
      val workload = worker.workload(bundle, cost)
      var updatedMind = mind.updateBelief(opponent, peerWorkload)
      updatedMind = mind.updateBelief(worker, workload)
      updatedMind = mind.add(task)
      if (trace) println(s"$worker -> $opponent : Cancel($task)")
      sender ! Cancel(task, workload)
      nbCancel += 1
      stay using updatedMind

  }

  /**
    * Or the agent is a proposer
    */
  when(Proposer, stateTimeout = deadline) {
    //If the deadline is reached
    case Event(StateTimeout, mind) =>
      self ! Trigger
      goto(Initial) using mind

    // If the worker agent receives a rejection
    case Event(Reject(task, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (opponent != mind.opponent || task != mind.task) {
        val updatedMind = mind.updateBelief(opponent, oWorkload)
        stay using updatedMind
      } else {
        var updatedMind = mind.updateBelief(opponent, oWorkload)
        updatedMind = updatedMind.changeDelegation(NoWorker, NoTask)
        self ! Trigger
        goto(Initial) using updatedMind
      }

    // If the worker agent receives an accptance
    case Event(Accept(task, oWorkload), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, oWorkload)
      if (opponent != mind.opponent || task != mind.task) {
        if (trace) println(s"$worker -> $opponent : Withdraw($task)")
        sender ! Withdraw(task, workload)
        nbWithdraw += 1
        stay using updatedMind
      } else {
        val workload = mind.belief(worker) - cost(worker, task)
        var updatedMind = mind.remove(task)
        updatedMind = updatedMind.updateBelief(worker, workload)
        updatedMind = updatedMind.changeDelegation(NoWorker, NoTask)
        if (trace) println(s"$worker -> $opponent : Confirm($task)")
        sender ! Confirm(task, workload)
        nbConfirm += 1
        broadcastInform(updatedMind.belief(worker))
        self ! Trigger
        goto(Initial) using updatedMind
      }

    // If the worker agent receives a proposal
    case Event(Propose(_, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a withdrawal
    case Event(Withdraw(_, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a confirmation
    case Event(Confirm(_, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a cancel
    case Event(Cancel(_, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a trigger
    case Event(Trigger, mind) =>
      stash
      stay using mind
  }

  /**
    * Or the agent waits for confirmation
    */
  when(Responder) { //  stateTimeout = deadline
    //If the deadline is reached
    /*case Event(StateTimeout, mind) =>
      unstashAll()
      self ! Trigger
      goto(Initial) using mind
    */
    // If the worker agent receives a confirmation
    case Event(Confirm(task, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.add(task)
      updatedMind = updatedMind.updateBelief(worker,  updatedMind.belief(worker) + cost(worker, task) )
      updatedMind = updatedMind.updateBelief(opponent, oWorkload)
      val workload = updatedMind.belief(worker)
      broadcastInform(workload)
      self ! Trigger
      goto(Initial) using updatedMind

    // If the  worker agent receives a withrawal
    case Event(Withdraw(_, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, oWorkload)
      self ! Trigger
      goto(Initial) using updatedMind


   // If the worker agent receives an acceptance
    case Event(Accept(task, oWorkload), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, oWorkload)
      if (trace) println(s"$worker -> $opponent : Withdraw($task)")
      sender ! Withdraw(task, workload)
      nbWithdraw += 1
      stay using updatedMind

    // If the worker agent receives a proposal
    case Event(Propose(task, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a rejection
    case Event(Reject(_, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a trigger
    case Event(Trigger, mind) =>
      stash
      stay using mind
  }

  /**
    * Whatever the state is
    **/
  whenUnhandled {
    // If the worker agent receives an inform
    case Event(Inform(opponent, workload), mind) =>
      if (rule == Cmax && workload < mind.belief(opponent)){
        self ! Trigger
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay using updatedMind

    // If the worker agent receives a query
    case Event(Query, mind) =>
      sender ! Finish(nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbCancel, nbInform)
      stay using mind

    // If the worker agent receives another message
    case Event (m: Message, mind) =>
      defaultReceive(m)
      stay using mind

    // In case of unexcpected event
    case Event (e, mind) =>
      println (s"${
        worker
      }: ERROR  unexpected event {} in state {}/{}", e, stateName, mind)
      stay using mind
  }

  //  Associates actions with a transition instead of with a state and even, e.g. debugging
  onTransition {
    case Initial -> Initial =>
      if (debug) println(s"$worker stay in Initial state")
    case Initial -> Proposer =>
      if (debug) println(s"$worker moves from Initial state to Proposer state")
    case Proposer -> Initial =>
      unstashAll()
      if (debug) println(s"$worker moves from Proposer state to Initial state")
    case Proposer -> Proposer =>
      if (debug) println(s"$worker stays in Proposer state")
    case Initial -> Responder =>
      unstashAll()
      if (debug) println(s"$worker moves from Initial state to Responder state")
    case Responder -> Initial =>
      if (debug) println(s"$worker moves from Responder state to Initial state")
    case Responder -> Responder =>
      if (debug) println(s"$worker stays in Responder state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}