// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.{FSM, Stash}
import org.scamata.core.{Worker, NoWorker$, NoTask, Task}
import org.scamata.solver._

import scala.collection.SortedSet
import scala.language.postfixOps

/**
  * Negotiation behaviour for a worker agent
  * @param worker   which is embedded
  * @param rule     to optimize
  * @param strategy for selecting deals
  */
class WorkerAgentBehaviour(worker: Worker, rule: SocialRule, strategy: DealStrategy)
  extends WorkerAgent(worker: Worker, rule: SocialRule, strategy: DealStrategy)
    with FSM[State, StateOfMind] with Stash {

  /**
    * Initially the worker is in the initial state with no bundle, no beliefs about the workloads and no responder for any particular task
    */
  startWith(Initial, new StateOfMind(SortedSet[Task](), Map[Worker, Double](), NoWorker$, NoTask, List[(Task,Worker)]() ) )

  /**
    * Either the worker is in Initial state
    */
  when(Initial) {
    // If the worker agent is triggered
    case Event(Trigger, mind) =>
      var updatedMind = mind
      val workload = updatedMind.belief(worker)
      if (sender == solverAgent) {
        // If the agent is triggered by the solverAgent
        broadcastInform(workload)
      }
       // Otherwise the mind is up to date
      val suppliers = rule match { // The potential suppliers are
        case LF => // all the peers
          directory.peers(worker)
        case LCmax => // the peers with a smallest workload
          directory.peers(worker).filter(j => updatedMind.belief(j) < workload)
      }
      // Either the worker has an empty bundle or no potential partners
      if (suppliers.isEmpty || updatedMind.bundle.isEmpty) {
        if (debug) println(s"$worker hopeless since $suppliers")
        solverAgent ! Stopped(updatedMind.bundle)
        stay using updatedMind
      } else { // Otherwise
        var found = false
        var bestSupplier = worker
        var bestTask: Task = NoTask
        var bestGoal = rule match {
          case LCmax =>
            workload
          case LF =>
            0.0
        }
        suppliers.foreach { supplier =>
          updatedMind.bundle.foreach { task => // foreach potential single gift
            val giftWorkload = workload - cost(worker, task)
            val giftSupplierWorkload = updatedMind.belief(supplier) + cost(supplier, task)
            val giftGoal = rule match {
              case LCmax =>
                Math.max(giftWorkload, giftSupplierWorkload)
              case LF =>
                cost(supplier, task) - cost(worker, task)
            }
            if (giftGoal < bestGoal && ! updatedMind.isBarred(task, supplier) ) {
              found = true
              bestGoal = giftGoal
              bestSupplier = supplier
              bestTask = task
            }
          }
        }
        if (!found) {
          solverAgent ! Stopped(updatedMind.bundle)
          stay using updatedMind
        } else {
          solverAgent ! Activated(updatedMind.bundle)
          val opponent = directory.adr(bestSupplier)
          updatedMind = updatedMind.changeDelegation(bestSupplier, bestTask)
          if (trace) println(s"$worker -> $bestSupplier : Propose($bestTask, $NoTask)")
          opponent ! Propose(bestTask, NoTask, workload)
          nbPropose += 1
          goto(Proposer) using updatedMind
        }
      }

    // If the worker agent receives a proposal TODO wiyh SingleSwapOnly
    case Event(Propose(task, NoTask, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, peerWorkload)
      val workload = updatedMind.belief(worker)
      val counterpart = bestCounterpart(task, opponent, updatedMind)
      if (counterpart != NoTask) {
        solverAgent ! Activated(updatedMind.bundle)
        updatedMind = updatedMind.changeDelegation(opponent, counterpart)
        if (trace) println(s"$worker -> $opponent : Propose($counterpart, $task)")
        sender ! Propose(counterpart, task, workload)
        nbCounterPropose += 1
        goto(Proposer) using updatedMind

      } else if (acceptable(task, provider = opponent, supplier = worker, updatedMind) && strategy != SingleSwapOnly) {
        solverAgent ! Activated(updatedMind.bundle)
        if (trace) println(s"$worker -> $opponent : Accept($task, $NoTask)")
        sender ! Accept(task, NoTask, updatedMind.belief(worker))
        nbAccept += 1
        goto(Responder) using updatedMind
      } else {
        if (trace) println(s"$worker -> $opponent : Reject($task, $NoTask)")
        sender ! Reject(task, NoTask, workload)
        nbReject += 1
        goto(Initial) using updatedMind
      }

    // If the worker agent receives a deprecated counter-proposal
    case Event(Propose(task, counterpart, peerWorkload), mind) if counterpart != NoTask =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, peerWorkload)
      val workload = updatedMind.belief(worker)
      if (trace) println(s"$worker -> $opponent : Reject($task, $counterpart)")
      sender ! Reject(task, counterpart, workload)
      nbReject += 1
      goto(Initial) using updatedMind

    // If the worker agent receives a deprecated acceptance
    case Event(Accept(task, counterpart, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
      val workload = updatedMind.belief(worker)
      if (trace) println(s"$worker -> $opponent : Withdraw($task, $counterpart)")
      sender ! Withdraw(task, counterpart, workload)
      nbWithdraw += 1
      stay using updatedMind

    // If the worker agent receives a deprecated rejection
    case Event(Reject(_, _, peerWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, peerWorkload)
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
    case Event(Reject(task, _, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, oWorkload)
      if (opponent != mind.responder || task != mind.task) {
        stay using updatedMind
      } else {
        updatedMind= updatedMind.barred(task, opponent)
        updatedMind = updatedMind.changeDelegation(NoWorker$, NoTask)
        self ! Trigger
        goto(Initial) using updatedMind
      }

    // If the worker agent receives an acceptance
    case Event(Accept(task, counterpart, oWorkload), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, oWorkload)
      if (opponent != mind.responder || task != mind.task) {
        if (trace) println(s"$worker -> $opponent : Withdraw($task, $counterpart)")
        sender ! Withdraw(task, counterpart, workload)
        nbWithdraw += 1
        stay using updatedMind
      } else {
        val workload = updatedMind.belief(worker) - cost(worker, task) + cost(worker, counterpart)
        updatedMind = updatedMind.remove(task)
        if (counterpart != NoTask) updatedMind = updatedMind.add(counterpart)
        updatedMind = updatedMind.updateBelief(worker, workload)
        updatedMind = updatedMind.changeDelegation(NoWorker$, NoTask)
        if (trace) println(s"$worker -> $opponent : Confirm($task, $counterpart)")
        sender ! Confirm(task, counterpart, workload)
        if (counterpart == NoTask)nbConfirmGift += 1
        else nbConfirmSwap += 1
        if (rule == LCmax) broadcastInform(updatedMind.belief(worker))
        self ! Trigger
        goto(Initial) using updatedMind
      }

    // If the worker agent receives a proposal
    case Event(Propose(task, counterpart, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      val workload = mind.belief(worker)
      var updatedMind = mind.updateBelief(opponent, oWorkload)
      if (opponent != updatedMind.responder || counterpart != updatedMind.task) {
          if (rnd.nextInt(100)<=forgetRate) {
            if (trace) println(s"$worker -> $opponent : Reject($task, $counterpart)")
            sender ! Reject(task, counterpart, workload)
            nbReject += 1
          } else {
            stash()
          }
          stay using updatedMind
      }
       else {
          if (acceptable(task, counterpart, provider = opponent, supplier = worker, updatedMind)) {
            solverAgent ! Activated(updatedMind.bundle)
            updatedMind = updatedMind.changeDelegation(NoWorker$, NoTask)
            if (trace) println(s"$worker -> $opponent : Accept($task, $counterpart)")
            sender ! Accept(task, counterpart, workload)
            nbAccept += 1
            goto(Responder) using updatedMind
          } else {// Not acceptable
            if (trace) println(s"$worker -> $opponent :  Reject($task, $counterpart)")
            sender ! Reject(task, counterpart, workload)
            nbReject += 1
            self ! Trigger
            goto(Initial) using updatedMind
          }
        }

    // If the worker agent receives a trigger
    case Event(Trigger, mind) =>
      stash
      stay using mind
  }

  /**
    * Or the agent waits for confirmation
    */
  when(Responder) {

    // If the worker agent receives a confirmation
    case Event(Confirm(task, counterpart, oWorkload), mind) =>
      var workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      var updatedMind = mind.add(task)
      updatedMind = updatedMind.remove(counterpart)
      updatedMind = updatedMind.updateBelief(worker, workload + cost(worker, task) - cost(worker, counterpart))
      updatedMind = updatedMind.updateBelief(opponent, oWorkload)
      workload = updatedMind.belief(worker)
      if (rule == LCmax) broadcastInform(workload)
      self ! Trigger
      goto(Initial) using updatedMind

    // If the  worker agent receives a withdrawal
    case Event(Withdraw(_, _, oWorkload), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, oWorkload)
      self ! Trigger
      goto(Initial) using updatedMind

    // If the worker agent receives a deprecated acceptance
    case Event(Accept(task, counterpart, oWorkload), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, oWorkload)
      if (trace) println(s"$worker -> $opponent : Withdraw($task, $counterpart)")
      sender ! Withdraw(task, counterpart, workload)
      nbWithdraw += 1
      stay using updatedMind

    // If the worker agent receives a proposal
    case Event(Propose(_, _, _), mind) =>
      stash
      stay using mind

    // If the worker agent receives a deprecated rejection
    case Event(Reject(_, _, _), mind) =>
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
    // If the worker agent is initiated
    case Event(Initiate(bundle, d, c), mind) =>
      this.solverAgent = sender
      this.costMatrix = c
      this.directory = d
      var updatedMind = mind.addBundle(bundle)
      val workload = worker.workload(bundle, costMatrix)
      updatedMind = updatedMind.initBelief(bundle, directory.allWorkers())
      updatedMind = updatedMind.updateBelief(worker, workload)
      sender ! Ready
      stay using updatedMind

    // If the worker agent receives an inform
    case Event(Inform(opponent, workload), mind) =>
      if (rule == LCmax && workload < mind.belief(opponent)) {
        self ! Trigger
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay using updatedMind

    // If the worker agent receives a query
    case Event(Query, mind) =>
      sender ! Finish(nbPropose, nbCounterPropose, nbAccept, nbReject, nbWithdraw, nbConfirmGift, nbConfirmSwap, nbCancel, nbInform)
      stay using mind

    // If the worker agent receives another message
    case Event(m: Message, mind) =>
      defaultReceive(m)
      stay using mind

    // In case of unexcpected event
    case Event(e, mind) =>
      println(s"${
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
    case Proposer -> Responder =>
      unstashAll()
      if (debug) println(s"$worker moves from Proposer state to Responder state")
    case Proposer -> Proposer =>
      if (debug) println(s"$worker stays in Proposer state")
    case Initial -> Responder =>
      if (debug) println(s"$worker moves from Initial state to Responder state")
    case Responder -> Initial =>
      unstashAll()
      if (debug) println(s"$worker moves from Responder state to Initial state")
    case Responder -> Responder =>
      if (debug) println(s"$worker stays in Responder state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}