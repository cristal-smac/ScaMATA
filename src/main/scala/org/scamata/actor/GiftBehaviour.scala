// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}
import org.scamata.deal.SingleGift

import scala.concurrent.duration._
import akka.actor.{FSM, Stash}
import scala.language.postfixOps
import scala.collection.SortedSet

/**
  * Agent behaviour
  * @param worker which is embedded
  * @param rule to optimize
  */
class GiftBehaviour(worker: Worker, rule: SocialRule) extends Agent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] with Stash{

  /**
    * Initially the worker is in responder state with no bundle and no beliefs about the workloads
    */
  startWith(Responder, new StateOfMind(SortedSet[Task](), Map[Worker, Double]()))

  /**
    * Either the worker is in responder state
    */
  when(Responder) {
    // If the worker is initiated
    case Event(Initiate(d, c), _) => // Initiate the directory and the cost matrix
      this.directory = d
      this.cost = c
      if (extraDebug) println(s"$worker Cost Matrix:\n $cost")
      stay using new StateOfMind(SortedSet[Task](), directory.allWorkers().map(w => (w, 0.0)).toMap)

    // If the worker is triggered
    case Event(Give(bundle), mind) =>
      var workload = mind.belief(worker)
      var updatedMind = mind
      if (sender == supervisor) {// If the agent is triggered by the supervisor
        updatedMind = mind.updateBundle(bundle)
        workload = worker.workload(updatedMind.bundle, cost)
        updatedMind = updatedMind.updateBelief(worker, workload)
        if (debug) println(s"$worker receives $updatedMind in responder state")
      }
      // Otherwise the mind is up to date
      broadcastInform(workload)
      // The potential partners are
      val potentialPartners = rule match {
        case Flowtime => // all the workers
          directory.peers(worker)
        case Cmax => // the workers with a smallest workload
          directory.peers(worker).filter(updatedMind.belief(_) < workload)
      }
      if (debug) println(s"$worker$updatedMind has potential partner: $potentialPartners")

      // Either the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty) {
        if (debug) println(s"$worker$updatedMind stays in responder state since it has an empty bundle or no potential partners")
        supervisor ! Stopped(updatedMind.bundle)
        stay using updatedMind
      } else { // Otherwise
        var found = false
        var bestBundle = updatedMind.bundle
        var bestSingleGift: SingleGift = new SingleGift(worker, worker, updatedMind.bundle.head)
        var bestGoal : Double = rule match { // The goal consists of
          case Cmax => //  decreasing the Cmax, i.e. its workload
            workload
          case Flowtime => // decreasing the flow time
            0.0
        }
        potentialPartners.foreach { opponent =>
          updatedMind.bundle.foreach { task =>
            // Foreach single swap
            val gift = new SingleGift(provider = worker, supplier = opponent, task)
            val giftBundle = updatedMind.bundle - task
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = updatedMind.belief(opponent) + cost(opponent, task)
            val giftGoal : Double = rule match {
              case Cmax => // the local Cmax
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime => // the decreasing flowtime
                cost(opponent, task) - cost(worker, task)
            }
            if (giftGoal < bestGoal) { // Allow swap which does not undermine the goal
              found = true
              if (debug) println(s"$worker$mind decision rule for $task with $opponent and goal $giftGoal")
              bestGoal = giftGoal
              bestSingleGift = gift
              bestBundle = giftBundle
            }
          }
        }
        if (! found) {
          if (debug) println(s"$worker$updatedMind stops in responder state")
          supervisor ! Stopped(updatedMind.bundle)
          goto(Responder) using updatedMind
        } else {
          val opponent = directory.adr(bestSingleGift.supplier)
          if (debug)  println(s"$worker$updatedMind restarts in proposer state")
          supervisor ! ReStarted(updatedMind.bundle)
          if (debug) println(s"$worker$updatedMind proposes ${bestSingleGift.task} to ${bestSingleGift.supplier}")
          opponent ! Propose(bestSingleGift.task, workload)
          goto(Proposer) using updatedMind
        }
      }

    // If the worker receives a proposal
    case Event(Propose(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (extraDebug) println(s"$worker$mind receives the proposal $task from $opponent in responder state")
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (acceptable(task, provider = opponent, supplier = worker, updatedMind)) {
        updatedMind = mind.updateBundle(mind.bundle + task)
        updatedMind = updatedMind.updateBelief(worker,  + cost(worker, task) )
        updatedMind = updatedMind.updateBelief(opponent, updatedMind.belief(opponent) - cost(opponent, task) )
        if (debug)  println(s"$worker$updatedMind restarts in proposer state")
        supervisor ! ReStarted(updatedMind.bundle)
        if (debug) println(s"$worker$updatedMind accepts the proposal $task from $opponent")
        sender ! Accept(task, updatedMind.belief(worker))
        if (debug) println(s"$worker$updatedMind broadcast its updated workload")
        broadcastInform(updatedMind.belief(worker))
        self ! Give(updatedMind.bundle)
        goto(Responder) using updatedMind
      }else{
        val workload = updatedMind.belief(worker)
        if (debug) println(s"$worker$updatedMind rejects $task from $opponent")
        sender ! Reject(task, workload)
        goto(Responder) using updatedMind
      }
    // If the worker receives a rejection
    case Event(Reject(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated reject")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind
    // If the worker receives an acceptance
    case Event(Accept(task, opponentWorkload), mind) =>
      if (extraDebug) println(s"$worker$mind receives a deprecated accept")
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind
  }

  /**
    * Or the worker is a proposer
    */
  when(Proposer, stateTimeout = 300 nanosecond) {
    case Event(StateTimeout, mind) =>
      if (debug) println(s"$worker$mind proposal timeout")
      self ! Give(mind.bundle)
      goto(Responder) using mind

    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (extraDebug) println(s"$worker$mind receives a rejection of $task from $opponent in proposer state")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind

    case Event(Accept(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (extraDebug) println(s"$worker$mind receives an acceptance of $task from $opponent in proposer state ")
      val workload = mind.belief(worker) - cost(worker, task)
      var updatedMind= mind.updateBundle(mind.bundle - task)
      updatedMind = updatedMind.updateBelief(worker, workload)
      updatedMind = updatedMind.updateBelief(opponent, opponentWorkload)
      if (debug) println(s"$worker$updatedMind broadcast its updated workload")
      broadcastInform(workload)
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind

    case Event(Propose(task, _), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker$mind receives a proposal of $task from $opponent in proposer state")
      stash
      stay

    case Event(Give(bundle), _) =>
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