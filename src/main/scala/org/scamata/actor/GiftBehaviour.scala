// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.{FSM, Stash}
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
    * Initially the worker is in Pause with no bundle, no beliefs about the workloads
    */
  startWith(Pause, new StateOfMind(SortedSet[Task](), Map[Worker, Double]()))

  /**
    * Either the worker is in Pause
    */
  when(Pause) {
    // If the worker is initiated
    case Event(Initiate(d, c), _) => // Initiate the directory and the cost matrix
      this.directory = d
      this.cost = c
      if (debug) println(s"$worker Cost Matrix:\n $cost")
      stay using new StateOfMind(SortedSet[Task](), directory.allWorkers().map(w => (w, 0.0)).toMap)

    // If the worker is triggered
    case Event(Give(bundle), mind) =>
      if (debug) println(s"$worker receives $bundle in state pause")
      val workload = worker.workload(bundle, cost)
      val flowtime = directory.peers(worker).foldLeft(0.0)((acc, w) => acc + mind.belief(w))
      if (debug) println(s"$worker's workload: $workload")
      val updatedBelief = mind.belief + (worker -> workload)
      broadcastInform(workload)
      // The potential partners are
      val potentialPartners = rule match {
        case Flowtime => // all the workers
          directory.peers(worker)
        case Cmax => // the workers with a smallest workload
          directory.peers(worker).filter(mind.belief(_) < workload)

      }
      if (debug) println(s"$worker has potential partner: $potentialPartners")
      if (potentialPartners.isEmpty || mind.bundle.isEmpty) { // Either the worker has an empty bundle or no potential partners"
        if (debug) println(s"$worker stays in Pause since he has an empty bundle or no potential partners")
        supervisor ! Stopped(bundle)
        stay using new StateOfMind(bundle, updatedBelief)
      } else { // Otherwise
        var found = false
        var bestBundle = bundle
        var bestSingleGift: SingleGift = new SingleGift(worker, worker, bundle.head)
        var bestGoal = rule match { // The goal consists of
          case Cmax => //  decreasing the Cmax, i.e. its workload
            workload
          case Flowtime => // decreasing the flow time
            flowtime
        }
        potentialPartners.foreach { opponent =>
          bundle.foreach { task =>
            // Foreach single swap
            val gift = new SingleGift(worker, opponent, task)
            val giftBundle = bundle - task
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = mind.belief(opponent)+cost(opponent, task)
            val giftGoal = rule match {
              case Cmax =>
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime =>
                flowtime - cost(worker, task) + cost(opponent, task)
            }
            if (giftGoal <= bestGoal) { // Allow swap which does not undermine the goal
              found = true
              bestGoal = giftWorkload
              bestSingleGift = gift
              bestBundle = giftBundle
            }
          }
        }
        if (! found) {
          if (debug) println(s"$worker stays in Pause")
          supervisor ! Stopped(bundle)
          goto(Pause) using new StateOfMind(bundle, updatedBelief)
        } else {
          if (debug) println(s"$bestSingleGift is proposed")
          directory.adr(bestSingleGift.supplier) ! Propose(bestSingleGift.task, workload)
          goto(Proposer) using new StateOfMind(bundle, updatedBelief)
        }
      }
    // If the worker receives a proposal
    case Event(Propose(task, w), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker receives a proposal $task from $opponent in state pause")
      var updatedBelief = mind.belief + (opponent -> w)
      if (acceptable(task, opponent, worker, updatedBelief)) {
        val updatedBundle = mind.bundle + task
        val workload = mind.belief(worker) + cost(worker, task)
        val opponentWorkload= w - cost(opponent, task)
        updatedBelief = mind.belief + (worker -> workload, opponent -> opponentWorkload )
        sender ! Accept(task, workload)
        self ! Give(updatedBundle)
        goto(Pause) using new StateOfMind(updatedBundle, updatedBelief)
      }else{
        val workload = mind.belief(worker)
        sender ! Reject(task, workload)
        goto(Pause) using new StateOfMind(mind.bundle, updatedBelief)
      }
  }

  /**
    * Or the worker is a proposer
    */
  when(Proposer) {
    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker receives a rejection of $task from $opponent in state Proposer")
      val updatedBelief = mind.belief +(opponent -> opponentWorkload)
      self ! Give(mind.bundle)
      goto(Pause) using new StateOfMind(mind.bundle, updatedBelief)

    case Event(Accept(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker receives an acceptance of $task from $opponent in state Proposer")
      val updatedBundle = mind.bundle - task
      val workload = mind.belief(worker) - cost(worker, task)
      val updatedBelief = mind.belief + (worker -> workload, opponent -> opponentWorkload )
      self ! Give(updatedBundle)
      goto(Pause) using new StateOfMind(updatedBundle, updatedBelief)

    case Event(Propose(task, _), _) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker receives a propsal of $task from $opponent in state Proposer")
      stash
      stay
  }



  /**
    * Whatever the state is
    **/
  whenUnhandled {
    case Event (Inform (opponent, workload), s) =>
      if (debug) println(s"$worker receives an inform from $opponent in state $s")
      val belief = s.belief + (worker -> workload)
      stay using new StateOfMind (s.bundle, belief)
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
    case Pause -> Pause => if (debug) println (s"$worker stay in pause")
    case Proposer -> Pause =>
      unstashAll()
      if (debug) println (s"$worker moves from proposer to stoppedActor")
    case Pause -> Proposer => if (debug) println (s"$worker moves from stoppedActor to proposer")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize ()

}