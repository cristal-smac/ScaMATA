// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import scala.concurrent.duration._
import akka.actor.{FSM, Stash}
import scala.language.postfixOps

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}
import org.scamata.deal.SingleGift

import scala.collection.SortedSet

/**
  * Agent behaviour
  * @param worker which is embedded
  * @param rule   to optimize
  */
class GiftBehaviour(worker: Worker, rule: SocialRule) extends Agent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] with Stash{

  /**
    * Initially the worker is in Responder with no bundle, no beliefs about the workloads
    */
  startWith(Responder, new StateOfMind(SortedSet[Task](), Map[Worker, Double]()))

  /**
    * Either the worker is in Responder
    */
  when(Responder) {
    // If the worker is initiated
    case Event(Initiate(d, c), _) => // Initiate the directory and the cost matrix
      this.directory = d
      this.cost = c
      //if (debug) println(s"$worker Cost Matrix:\n $cost")
      stay using new StateOfMind(SortedSet[Task](), directory.allWorkers().map(w => (w, 0.0)).toMap)

    // If the worker is triggered
    case Event(Give(bundle), mind) =>
      if (debug) println(s"$worker receives $bundle in responder state")
      var updatedMind = mind
      if (mind.bundle.isEmpty) {
        updatedMind = mind.updateBundle(bundle)
      }
      var workload = worker.workload(updatedMind.bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      broadcastInform(workload)
      //if (debug) println(s"$worker's workload: $workload")

      // The potential partners are
      val potentialPartners = rule match {
        case Flowtime => // all the workers
          directory.peers(worker)
        case Cmax => // the workers with a smallest workload
          directory.peers(worker).filter(updatedMind.belief(_) < workload)
      }
      if (debug) println(s"$worker has potential partner: $potentialPartners")

      // Either the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty) {
        if (debug) println(s"$worker stays in responder state since he has an empty bundle or no potential partners")
        supervisor ! Stopped(updatedMind.bundle)
        stay using updatedMind

      } else { // Otherwise
        var found = false
        var bestBundle = bundle
        var bestSingleGift: SingleGift = new SingleGift(worker, worker, updatedMind.bundle.head)
        var bestGoal = rule match { // The goal consists of
          case Cmax => //  decreasing the Cmax, i.e. its workload
            workload
          case Flowtime => // decreasing the flow time
            updatedMind.flowtime()
        }
        potentialPartners.foreach { opponent =>
          updatedMind.bundle.foreach { task =>
            // Foreach single swap
            val gift = new SingleGift(worker, opponent, task)
            val giftBundle = updatedMind.bundle - task
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = updatedMind.belief(opponent) + cost(opponent, task)
            val giftGoal = rule match {
              case Cmax =>
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime =>
                updatedMind.flowtime - cost(worker, task) + cost(opponent, task)
            }
            if (giftGoal < bestGoal) { // Allow swap which does not undermine the goal
              found = true
              bestGoal = giftWorkload
              bestSingleGift = gift
              bestBundle = giftBundle
            }
          }
        }
        if (! found) {
          if (debug) println(s"$worker stops in Responder")
          supervisor ! Stopped(updatedMind.bundle)
          goto(Responder) using updatedMind
        } else {
          val opponent = directory.adr(bestSingleGift.supplier)
          if (debug) println(s"$worker propose ${bestSingleGift.task} to ${bestSingleGift.supplier}")
          supervisor ! ReStarted(updatedMind.bundle)
          opponent ! Propose(bestSingleGift.task, workload)
          goto(Proposer) using updatedMind
        }
      }

    // If the worker receives a proposal
    case Event(Propose(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      //if (debug) println(s"$worker receives the proposal $task from $opponent in responder state")
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (acceptable(task, opponent, worker, updatedMind)) {
        if (debug) println(s"$worker accepts $task from $opponent")
        updatedMind = mind.updateBundle(mind.bundle + task)
        updatedMind = updatedMind.updateBelief(worker, updatedMind.belief(worker) + cost(worker, task) )
        updatedMind = updatedMind.updateBelief(opponent, opponentWorkload - cost(opponent, task) )
        if (debug) println(s"$worker accepts the proposal $task from $opponent")
        supervisor ! ReStarted(updatedMind.bundle)
        sender ! Accept(task, updatedMind.belief(worker))
        broadcastInform(updatedMind.belief(worker))
        self ! Give(updatedMind.bundle)
        goto(Responder) using updatedMind
      }else{
        if (debug) println(s"$worker rejects $task from $opponent")
        val workload = updatedMind.belief(worker)
        sender ! Reject(task, workload)
        goto(Responder) using updatedMind
      }
    case Event(Reject(task, opponentWorkload), mind) =>
      if (debug) println(s"$worker receives a deprecated reject")
      stay using mind
    case Event(Accept(task, opponentWorkload), mind) =>
      if (debug) println(s"$worker receives a deprecated accept")
      stay using mind
  }

  /**
    * Or the worker is a proposer
    */
  when(Proposer, stateTimeout = 300 nanosecond) {
    case Event(StateTimeout, mind) =>
      goto(Responder) using mind

    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      //if (debug) println(s"$worker receives a rejection of $task from $opponent in proposer state")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind

    case Event(Accept(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      //if (debug) println(s"$worker receives an acceptance of $task from $opponent in proposer state ")
      val workload = mind.belief(worker) - cost(worker, task)
      var updatedMind= mind.updateBundle(mind.bundle - task)
      updatedMind = updatedMind.updateBelief(worker, workload)
      updatedMind = updatedMind.updateBelief(opponent, opponentWorkload)
      broadcastInform(workload)
      self ! Give(updatedMind.bundle)
      goto(Responder) using updatedMind

    case Event(Propose(task, _), _) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker receives a proposal of $task from $opponent in proposer state")
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
      //if (debug) println(s"$worker receives an inform from $opponent in state $mind")
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
      //if (debug) println (s"$worker stay in responder state")
    case Proposer -> Responder =>
      unstashAll()
      //if (debug) println (s"$worker moves from Proposer to Responder")
    case Responder -> Proposer =>
      //if (debug) println (s"$worker moves from responder to proposer state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}