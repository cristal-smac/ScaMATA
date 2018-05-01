// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker, NoWorker, NoTask}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.concurrent.duration._
import akka.actor.{FSM, Stash}
import scala.language.postfixOps
import scala.collection.SortedSet

/**
  * Gift negotiation behaviour
  * @param worker which is embedded
  * @param rule to optimize
  */
class GiftBehaviour(worker: Worker, rule: SocialRule) extends Agent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] with Stash{

  /**
    * Initially the worker is in responder state with no bundle, no beliefs about the workloads and no proposer
    */
  startWith(Responder, new StateOfMind(SortedSet[Task](), Map[Worker, Double](), NoWorker, NoTask))

  /**
    * Either the worker is in responder state
    */
  when(Responder) {
    // If the worker is initiated
    case Event(Initiate(d, c), _) => // Initiate the directory and the cost matrix
      this.directory = d
      this.cost = c
      this.supervisor = sender
      if (extraDebug) println(s"$worker sends Ready to the supervisor")
      sender ! Ready
      stay using new StateOfMind(SortedSet[Task](), directory.allWorkers().map(w => (w, 0.0)).toMap, NoWorker, NoTask)
    // If the worker is triggered
    case Event(Give(bundle), mind) =>
      var updatedMind = mind
      var workload = updatedMind.belief(worker)
      if (sender == supervisor) {// If the agent is triggered by the supervisor
        updatedMind = mind.updateBundle(bundle)
        workload = worker.workload(updatedMind.bundle, cost)
        updatedMind = updatedMind.updateBelief(worker, workload)
        if (debug) println(s"$worker receives $updatedMind in responder state")
        if (extraDebug) println(s"$worker$updatedMind broadcast its updated workload")
        broadcastInform(workload)
      }
      // Otherwise the mind is up to date
      val potentialPartners = rule match { // The potential partners are
        case Flowtime => // all the workers
          directory.peers(worker)
        case Cmax => // the workers with a smallest workload
          directory.peers(worker).filter(updatedMind.belief(_) < workload)
      }
      if (extraDebug) println(s"$worker$updatedMind has potential partner: $potentialPartners")
      // Either the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty) {
        if (debug) println(s"$worker$updatedMind stops in responder state")
        supervisor ! Stopped(updatedMind.bundle)
        stay using updatedMind
      } else { // Otherwise
        var found = false
        var bestOpponent = worker
        var bestTask =  updatedMind.bundle.head
        var bestGoal = rule match { // The goal consists of
          case Cmax => //  decreasing the local cmax, i.e. its workload
            workload
          case Flowtime => // decreasing the flow time
            0.0
        }
        potentialPartners.foreach { opponent =>
          updatedMind.bundle.foreach { task => // foreach potential single swap
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = updatedMind.belief(opponent) + cost(opponent, task)
            val giftGoal = rule match {
              case Cmax => // the local Cmax
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime => // the decreasing flowtime
                cost(opponent, task) - cost(worker, task)
            }
            if (giftGoal < bestGoal) { // Allow swap which does not undermine the goal
              found = true
              if (extraDebug) println(s"$worker$updatedMind decision rule for $task with $opponent and goal $giftGoal")
              bestGoal = giftGoal
              bestOpponent = opponent
              bestTask = task
            }
          }
        }
        if (! found) {
          if (debug) println(s"$worker$updatedMind stops in responder state")
          supervisor ! Stopped(updatedMind.bundle)
          goto(Responder) using updatedMind
        } else {
          val opponent = directory.adr(bestOpponent)
          updatedMind = updatedMind.updateDelegation(bestOpponent, bestTask)
          if (extraDebug)  println(s"$worker$updatedMind restarts in proposer state")
          supervisor ! ReStarted(updatedMind.bundle)
          if (debug) println(s"$worker$updatedMind proposes $bestTask to $bestOpponent")
          opponent ! Propose(bestTask, workload)
          goto(Proposer) using updatedMind
        }
      }
    case Event(Propose(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (extraDebug) println(s"$worker$mind receives the proposal $task from $opponent in responder state")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (acceptable(task, provider = opponent, supplier = worker, updatedMind)) {
        if (extraDebug)  println(s"$worker$updatedMind restarts in proposer state")
        supervisor ! ReStarted(updatedMind.bundle)
        if (debug) println(s"$worker$updatedMind accepts the proposal $task from $opponent")
        sender ! Accept(task, updatedMind.belief(worker))
        goto(WaitConfirmation) using updatedMind
      }else{
        val workload = updatedMind.belief(worker)
        if (debug) println(s"$worker$updatedMind rejects $task from $opponent")
        sender ! Reject(task, workload)
        goto(Responder) using updatedMind
      }
    case Event(Reject(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated rejectiom")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind
    case Event(Accept(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated acceptance")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = worker.workload(updatedMind.bundle, cost)
      if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
      sender ! Withdraw(task, workload)
      stay using updatedMind
  }

  /**
    * Or the agent is a proposer
    */
  when(Proposer, stateTimeout = 300 nanosecond) {
    case Event(StateTimeout, mind) =>
      if (debug) println(s"$worker$mind proposal timeout")
      self ! Give(mind.bundle)
      goto(Responder) using mind
    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (opponent != mind.opponent || task != mind.task) {
        if (extraDebug) println(s"$worker$mind receives a deprecated rejection of $task from $opponent in proposer state ")
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        stay using updatedMind
      } else {
        if (extraDebug) println(s"$worker$mind receives a rejection of $task from $opponent in proposer state")
        var updatedMind = mind.updateBelief(opponent, opponentWorkload)
        updatedMind = updatedMind.updateDelegation(NoWorker, NoTask)
        self ! Give(updatedMind.bundle)
        goto(Responder) using updatedMind
      }
    case Event(Accept(task, opponentWorkload), mind) =>
      var workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      if (opponent != mind.opponent || task != mind.task) {
        if (extraDebug) println(s"$worker$mind receives a deprecated acceptance of $task from $opponent in proposer state ")
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
        sender ! Withdraw(task, workload)
        stay using updatedMind
      } else {
        if (extraDebug) println(s"$worker$mind receives an acceptance of $task from $opponent in proposer state ")
        val workload = mind.belief(worker) - cost(worker, task)
        var updatedMind = mind.remove(task)
        updatedMind = updatedMind.updateBelief(worker, workload)
        updatedMind = updatedMind.updateBelief(opponent, opponentWorkload)
        updatedMind = updatedMind.updateDelegation(NoWorker, NoTask)
        if (debug) println(s"$worker$updatedMind confirms $task delegation to $opponent")
        sender ! Confirm(task, workload)
        if (extraDebug) println(s"$worker$updatedMind broadcast its updated workload")
        broadcastInform(updatedMind.belief(worker))
        self ! Give(updatedMind.bundle)
        goto(Responder) using updatedMind
      }
    case Event(Propose(task, _), mind) =>
      val opponent = directory.workers(sender)
      if (extraDebug) println(s"$worker$mind receives a proposal of $task from $opponent in proposer state")
      stash
      stay
    case Event(Give(_), _) =>
      stash
      stay
  }

  /**
    * Or the agent waits for confirmation
    */
  when(WaitConfirmation) {
    case Event(Confirm(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.add(task)
      updatedMind = updatedMind.updateBelief(worker,  updatedMind.belief(worker) + cost(worker, task) )
      updatedMind = updatedMind.updateBelief(opponent, updatedMind.belief(opponent) - cost(opponent, task) )
      if (extraDebug) println(s"$worker$updatedMind broadcast its updated workload")
      broadcastInform(updatedMind.belief(worker))
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind
    case Event(Withdraw(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, mind.belief(opponent))
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind
    case Event(Reject(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated rejection")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind
    case Event(Accept(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated acceptance")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = worker.workload(updatedMind.bundle, cost)
      if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
      sender ! Withdraw(task, workload)
      stay using updatedMind
    case Event(Propose(task, _), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker$mind receives a proposal of $task from $opponent in proposer state")
      stash
      stay
    case Event(Give(_), _) =>
      stash
      stay
  }

  /**
    * Whatever the state is
    **/
  whenUnhandled {
    case Event (Inform (opponent, workload), mind) =>
      if (extraDebug) println(s"$worker$mind receives an inform from $opponent in state $mind")
      val updatedMind = mind.updateBelief(opponent, workload)
      stay using updatedMind
    case Event (m: Message, _) =>
      defaultReceive(m)
      stay
    case Event (e, s) =>
      println (s"${
        worker
      }: ERROR  unexpected event {} in state {}/{}", e, stateName, s)
      stay
  }

  //  Associates actions with a transition instead of with a state and even, e.g. debugging
  onTransition {
    case Responder -> Responder =>
      if (extraDebug) println (s"$worker stay in responder state")
    case Proposer -> Responder =>
      unstashAll()
      if (extraDebug) println (s"$worker moves from proposer to responder state")
    case Responder -> Proposer =>
      if (extraDebug) println (s"$worker moves from responder to proposer state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}