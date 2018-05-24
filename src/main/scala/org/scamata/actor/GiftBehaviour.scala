// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{NoWorker, Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}
import akka.actor.{FSM, Stash}

import scala.language.postfixOps
import scala.collection.SortedSet

/**
  * Gift negotiation behaviour
  * @param worker which is embedded
  * @param rule to optimize
  */
class GiftBehaviour(worker: Worker, rule: SocialRule) extends Agent(worker: Worker, rule: SocialRule) with FSM[State, StateOfMind] with Stash{

  var deseperated = false // Does the agent has tried all the potential gifts


  /**
    * Initially the worker is in responder state with no bundle, no beliefs about the workloads and no potential supplier/task
    */
  startWith(Responder, new StateOfMind(SortedSet[Task](), Map[Worker, Double](), conversationId = 0))

  /**
    * Either the worker is in responder state
    */
  when(Responder) {
    case Event(Initiate(bundle, d, c), mind) => // Initiate the bundle the directory and the cost matrix
      supervisor = sender// I am you father, Luke
      this.directory = d
      this.cost = c
      var updatedMind = mind.initBelief(directory.allWorkers())
      updatedMind = updatedMind.addBundle(bundle)
      val workload = worker.workload(bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      sender ! Ready
      stay using updatedMind

    case Event(Trigger, mind) =>
      var updatedMind = mind
      val workload = updatedMind.belief(worker)
      // If the agent is triggered by the supervisor and the rule is Cmax
      if (sender == supervisor && rule == Cmax) {
        nbInform +=1
        broadcastInform(workload)
      }
      // Otherwise the mind is up to date
      val potentialPartners = rule match { // The potential partners are
        case Flowtime => // all the peers
          directory.peers(worker)
        case Cmax => // the peers with a smallest workload
          directory.peers(worker).filter(updatedMind.belief(_) < workload)
      }
      // Either the agent is deseperated, i.e. the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty){
        deseperated = true
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
          updatedMind.bundle.foreach { task => //foreach potential single swap
            val giftWorkload = workload - cost(worker, task)
            val giftOpponentWorkload = updatedMind.belief(opponent) + cost(opponent, task)
            val giftGoal = rule match{
              case Cmax => // the local Cmax
                Math.max( giftWorkload, giftOpponentWorkload )
              case Flowtime => // the task cost
                cost(opponent, task) - cost(worker, task)
            }
            if (giftGoal < bestGoal){
              found = true
              bestGoal = giftGoal
              bestOpponent = opponent
              bestTask = task
            }
          }
        }
        if (! found) {
          deseperated = true
          if (debug) println(s"$worker -> Supervisor : Stop$updatedMind")
          supervisor ! Stopped(updatedMind.bundle)
          stay using updatedMind
        } else {
          val opponent = directory.adr(bestOpponent)
          deseperated = false
          if (debug) println(s"$worker -> Supervisor : ReStart(...)")
          supervisor ! ReStarted(updatedMind.bundle)
          updatedMind = updatedMind.updateConversationId(updatedMind.conversationId+1)
          if (debug) println(s"$worker -> $bestOpponent : Propose($bestTask) " +
            s"since max($workload -  ${cost(worker, bestTask)}, ${updatedMind.belief(bestOpponent)} + ${cost(bestOpponent, bestTask)}) > max($workload, ${updatedMind.belief(bestOpponent)})")
          opponent ! Propose(bestTask, workload, updatedMind.conversationId)
          nbPropose += 1
          goto(Proposer) forMax randomTimeout using updatedMind
        }
      }

    case Event(Propose(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (acceptable(task, provider = opponent, supplier = worker, updatedMind)) {
        deseperated = false
        if (debug) println(s"$worker -> Supervisor : ReStart(...)")
        supervisor ! ReStarted(updatedMind.bundle)
        if (debug) println(s"$worker -> $opponent : Accept($task) " +
          s"since max(${updatedMind.belief(worker)} + ${cost(worker, task)} , $opponentWorkload - ${cost(opponent, task)}) < max(${updatedMind.belief(worker)} , $opponentWorkload")
        updatedMind = updatedMind.updateConversationId(updatedMind.conversationId+1)
        sender ! Accept(task, updatedMind.belief(worker), id)
        nbAccept += 1
        goto(WaitConfirmation) forMax randomTimeout using updatedMind
      }else{
        val workload = updatedMind.belief(worker)
        if (debug) println(s"$worker -> $opponent : Reject($task)")
        sender ! Reject(task, workload, id)
        nbReject +=1
        goto(Responder) using updatedMind
      }

    case Event(Reject(_, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind

    case Event(Accept(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = updatedMind.belief(worker)
      if (debug) println(s"$worker -> $opponent: Withdraw($task)")
      sender ! Withdraw(task, workload, id)
      nbWithdraw +=1
      stay using updatedMind

    case Event(Confirm(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = updatedMind.belief(worker)
      if (debug) println(s"$worker -> $opponent: Cancel($task)")
      sender ! Cancel(task, workload, id)
      nbCancel +=1
      if (debug) println(s"$worker -> $worker: Trigger")
      stay using updatedMind

    case Event(Withdraw(_, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (debug) println(s"$worker -> $worker: Trigger")
      stay using updatedMind

    case Event(Cancel(task, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      updatedMind = updatedMind.remove(task)
      val workload = worker.workload(updatedMind.bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      if (debug) println(s"$worker -> $worker: Trigger")
      stay using updatedMind

    case Event(Inform(opponent, workload), mind) =>
      if (rule == Cmax && workload < mind.belief(opponent)){
        deseperated = false
        if (debug) println(s"$worker -> Supervisor: ReStart(...)")
        supervisor ! ReStarted(mind.bundle)
        self ! Trigger
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay using updatedMind
  }

  /**
    * Or the agent is a proposer
    */
  when(Proposer) {//
    case Event(StateTimeout, mind) =>
      unstashAll()
      self ! Trigger
      if (debug) println(s"$worker -> $worker: Trigger")
      goto(Responder) using mind

    case Event(Reject(_, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      if (id != mind.conversationId) {
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        stay forMax randomTimeout using updatedMind
      } else {
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        unstashAll()
        if (debug) println(s"$worker -> $worker: Trigger")
        self ! Trigger
        goto(Responder) using updatedMind
      }

    case Event(Accept(task, opponentWorkload, id), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      if (id != mind.conversationId) {
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        if (debug) println(s"$worker -> $opponent: Withdraw($task)")
        sender ! Withdraw(task, workload, id)
        nbWithdraw +=1
        stay forMax randomTimeout using updatedMind
      } else {
        var updatedMind = mind.remove(task)
        val workload = mind.belief(worker) - cost(worker, task)
        updatedMind = updatedMind.updateBelief(worker, workload)
        updatedMind = updatedMind.updateBelief(opponent, opponentWorkload + cost(opponent, task))
        sender ! Confirm(task, workload, id)
        nbConfirm +=1
        if (rule == Cmax) {
          broadcastInform(updatedMind.belief(worker))
          nbInform += 1
        }
        unstashAll()
        if (debug) println(s"$worker -> $worker: Trigger")
        self ! Trigger
        goto(Responder) using updatedMind
      }

    case Event(Propose(_, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stash
      stay forMax randomTimeout using updatedMind

    case Event(Confirm(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = updatedMind.belief(worker)
      if (debug) println(s"$worker -> $opponent: Cancel($task)")
      sender ! Cancel(task, workload, id)
      nbCancel +=1
      stay forMax randomTimeout using updatedMind

    case Event(Withdraw(_, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay forMax randomTimeout using updatedMind

    case Event(Cancel(task, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      updatedMind = updatedMind.remove(task)
      val workload = worker.workload(updatedMind.bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      stay forMax randomTimeout using updatedMind

    case Event(Trigger, mind) =>
      stash
      stay forMax randomTimeout using mind

    case Event(Inform(opponent, workload), mind) =>
      if (rule == Cmax && workload < mind.belief(opponent)){
        deseperated = false
        if (debug) println(s"$worker -> Supervisor: ReStart(...)")
        supervisor ! ReStarted(mind.bundle)
        self ! Trigger
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay forMax randomTimeout using updatedMind
  }

  /**
    * Or the agent waits for confirmation
    */
  when(WaitConfirmation) {
    case Event(StateTimeout, mind) =>
      if (debug) println(s"$worker -> $worker: Trigger")
      unstashAll()
      self ! Trigger
      goto(Responder) using mind

    case Event(Confirm(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (id != mind.conversationId){
        val opponent = directory.workers(sender)
        val workload = updatedMind.belief(worker)
        if (debug) println(s"$worker -> $opponent: Cancel($task)")
        sender ! Cancel(task, workload, id)
        nbCancel +=1
        stay forMax randomTimeout using updatedMind
      }
      else {
        updatedMind = mind.add(task)
        updatedMind = updatedMind.updateBelief(worker, updatedMind.belief(worker) + cost(worker, task))
        if (rule == Cmax) {
          broadcastInform(updatedMind.belief(worker))
          nbInform += 1
        }
        unstashAll()
        if (debug) println(s"$worker -> $worker: Trigger")
        self ! Trigger
        goto(Responder) using updatedMind
      }

    case Event(Withdraw(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, mind.belief(opponent))
      if (id != updatedMind.conversationId){
        stay forMax randomTimeout using updatedMind
      }else{
        unstashAll()
        if (debug) println(s"$worker -> $worker: Trigger")
        self ! Trigger
        goto(Responder) using updatedMind
      }

    case Event(Reject(_, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay forMax randomTimeout  using updatedMind

    case Event(Accept(task, opponentWorkload, id), mind) =>
      val opponent = directory.workers(sender)
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      //TODO
      if (debug) println(s"$worker -> $opponent: Withdraw($task)")
      val workload = updatedMind.belief(worker)
      sender ! Withdraw(task, workload, id)
      nbWithdraw +=1
      stay forMax randomTimeout using updatedMind

    case Event(Propose(_, _, _), mind) =>
      stash
      stay forMax randomTimeout using mind

    case Event(Cancel(task, opponentWorkload, _), mind) =>
      val opponent = directory.workers(sender)
      var updatedMind = mind.updateBelief(opponent, opponentWorkload)
      updatedMind = updatedMind.remove(task)
      val workload = worker.workload(updatedMind.bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      stay forMax randomTimeout using updatedMind

    case Event(Trigger, mind) =>
      stash
      stay forMax randomTimeout using mind

    case Event(Inform(opponent, workload), mind) =>
      if (rule == Cmax && workload < mind.belief(opponent)){
        deseperated = false
        if (debug) println(s"$worker -> Supervisor: ReStart(...)")
        supervisor ! ReStarted(mind.bundle)
        self ! Trigger
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay forMax randomTimeout using updatedMind

  }

  /**
    * Whatever the state is
    **/
  whenUnhandled{

    case Event(Query, mind) =>
      if (deseperated) sender ! Finish(nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbInform)
      stay using mind

    case Event (m: Message, mind) =>
      defaultReceive(m)
      stay using mind

    case Event (e, mind) =>
      println (s"${
        worker
      }: ERROR  unexpected event {} in state {}/{}", e, stateName, mind)
      stay using mind
  }

  //  Associates actions with a transition instead of with a state and even, e.g. debugging
  onTransition {
    case Responder -> Responder =>
      if (stateDebug) println (s"$worker stay in responder state")
    case WaitConfirmation -> WaitConfirmation =>
      if (stateDebug) println (s"$worker stay in waiting confirmation state")
    case Proposer -> Responder =>
      if (stateDebug) println (s"$worker moves from proposer to responder state")
    case WaitConfirmation -> Responder =>
      if (stateDebug) println (s"$worker moves from waiting confirmation state to responder state")
    case Responder -> Proposer =>
      if (stateDebug) println (s"$worker moves from responder to proposer state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}