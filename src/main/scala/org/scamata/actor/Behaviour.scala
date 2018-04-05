// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.FSM
import org.scamata.core.{Task, Worker}
import org.scamata.solver.SocialRule
import org.scamata.deal.SingleGift

import scala.collection.SortedSet

/**
  * Agent behaviour
  *
  * @param worker as a worker
  * @param rule   to optimize
  */
class Behaviour(worker: Worker, rule: SocialRule) extends Agent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] {

  /**
    * Initially the agent is in pause with no idea about the workloads
    */
  startWith(Pause, new StateOfMind(SortedSet[Task](), directory.allWorkers(), directory.allWorkers().map(w => (w, 0.0)).toMap))


  /**
    * Either the agent is in pause
    */
  when(Pause) {
    case Event(Give(bundle), mind) =>
      val myWorkload = worker.workload(bundle, cost)
      broadcastInform(myWorkload)
      val potentialPartners = mind.workers.filter(mind.workload(_) < myWorkload)
      if (debug) println(s"Potential partner: $potentialPartners")
      if (potentialPartners.isEmpty || mind.bundle.isEmpty) {
        if (debug) println(s"$worker becomes inactive")
        supervisor ! Stopped(mind.bundle)
        goto(Pause) using mind
      } else {
        var bestBundle = bundle
        var bestGoal = mind.workload(worker)
        var bestSingleGift: SingleGift = new SingleGift(worker, worker, bundle.head)
        potentialPartners.foreach { opponent =>
          bundle.foreach { task =>
            val gift = new SingleGift(worker, opponent, task)
            val modifiedBundle = bundle - task
            val currentGoal = worker.workload(modifiedBundle, cost)
            if (currentGoal < bestGoal) {
              bestGoal = currentGoal
              bestSingleGift = gift
              bestBundle = modifiedBundle
            }
          }
        }
        // Select the best gift if any
        if (bestBundle.equals(bundle)) {
          if (debug) println(s"$worker becomes inactive")
          supervisor ! Stopped(mind.bundle)
          goto(Pause) using mind
        } else {
          if (debug) println(s"$bestSingleGift is performed")
          directory.adr(bestSingleGift.supplier) ! Propose(bestSingleGift.task, mind.workload(worker))
          goto(Proposer) using mind
        }
      }
  }
  /*
        /**
          * Either the agent has make a proposal
          */
     when(Proposer) {
       case Event(CounterPropose(t,w), mind) => {
         workload(directory.name(sender))= w
         if (w < mind.workload ) {
           sender ! Accept(t, mind.workload)
           goto(Pause) using new StateOfMind(bundle - t, )

         }

         if (debug) log.debug(s"$i is assigned to ${concessions.head}")
         solver ! Assignement(i, concessions.head)
       }
       case Reject => {
         if (debug) log.debug(s"$i is rejected by ${concessions.head}")
         this.concede()
         if (this.isDesesperated) {//Either allActors concessions are made and i is inactive
           solver ! Assignement(i, Activity.VOID.name)
         }else {//Or he proposes to the next preferred activiyt
           addresses(this.preferredActiviy()) ! Propose(i)
         }
       }
       case Withdraw => {
         if (debug) log.debug(s"$i is ejected by ${concessions.head}")
         solver ! Disassignement(i)
       }

     }

      case Confirm => {
        if (debug) log.debug(s"$i has received  the confirmation of the dissagnement")
        addresses(preferredActiviy()) ! Confirm
        this.concede()
        if (this.isDesesperated) { // Either allActors concessions are made and i is inactive
          solver ! Assignement(i, Activity.VOID.name)
        } else { // Or concession is made
          if (debug)  log.debug(s"$i proposes itself to ${concessions.head}")
          addresses(this.preferredActiviy()) ! Propose(i)
        }
      }
      case Query(g,a) => { // Opinion is requested
        sender ! Reply(g,a,individual.u(g,a))
      }
    }

  */

  /**
    * Whatever the state is
    **/
  whenUnhandled {
    case Event(Inform(worker, workload), s) =>
      var belief = s.workload + (worker -> workload)
      stay using new StateOfMind(s.bundle, s.workers, belief)
    case Event(m: Message, s) =>
      defaultReceive(m)
      stay
    case Event(e, s) =>
      println(s"${worker}: ERROR  unexpected event {} in state {}/{}", e, stateName, s)
      stay
  }

  //  Associates actions with a transition instead of with a state and even, e.g. debugging
  onTransition {
    case _ -> _ => if (debug) println(s"$worker changes state with the bundle $nextStateData")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()

}