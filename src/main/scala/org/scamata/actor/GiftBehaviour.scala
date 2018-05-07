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

  var stopped = false

  /**
    * Initially the worker is in responder state with no bundle, no beliefs about the workloads and no potential supplier/task
    */
  startWith(Responder, new StateOfMind(SortedSet[Task](), Map[Worker, Double](), NoWorker, NoTask))

  /**
    * Either the worker is in responder state
    */
  when(Responder) {
    case Event(Initiate(bundle, d, c), mind) => // Initiate the bundle the directory and the cost matrix
      if (receiveDebug) println(s"$worker$mind is initiated")
      supervisor = sender// I am you father, Luke
      this.directory = d
      this.cost = c
      var updatedMind = mind.initBelief(directory.allWorkers())
      updatedMind = updatedMind.updateBundle(bundle)
      val workload = worker.workload(bundle, cost)
      updatedMind = updatedMind.updateBelief(worker, workload)
      if (debug) println(s"$worker sends Ready")
      sender ! Ready
      stay using updatedMind

    case Event(Start, mind) =>
      if (receiveDebug) println(s"$worker$mind is triggered")
      var updatedMind = mind
      val workload = updatedMind.belief(worker)
      if (sender == supervisor && rule == Cmax) {// If the agent is triggered by the supervisor and the rule us Cmax
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
      // Either the worker has an empty bundle or no potential partners
      if (potentialPartners.isEmpty || updatedMind.bundle.isEmpty) {
        if (debug) println(s"$worker$updatedMind stops in responder state")
        stopped = true
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
          if (debug) println(s"$worker$updatedMind stops in responder state")
          stopped = true
          supervisor ! Stopped(updatedMind.bundle)
          goto(Responder) using updatedMind
        } else {
          val opponent = directory.adr(bestOpponent)
          updatedMind = updatedMind.updateDelegation(bestOpponent, bestTask)
          stopped = false
          supervisor ! ReStarted(updatedMind.bundle)
          if (receiveDebug)  println(s"$worker$updatedMind restarts in proposer state")
          if (debug) println(s"$worker$updatedMind proposes $bestTask to $bestOpponent")
          opponent ! Propose(bestTask, workload)
          nbPropose += 1
          goto(Proposer) using updatedMind
        }
      }

    case Event(Propose(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives the proposal $task from $opponent in responder state")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      if (acceptable(task, provider = opponent, supplier = worker, updatedMind)) {
        stopped = false
        supervisor ! ReStarted(updatedMind.bundle)
        if (debug)  println(s"$worker$updatedMind restarts in proposer state")
        if (debug) println(s"$worker$updatedMind accepts the proposal $task from $opponent")
        sender ! Accept(task, updatedMind.belief(worker))
        nbAccept += 1
        goto(WaitConfirmation) using updatedMind
      }else{
        val workload = updatedMind.belief(worker)
        if (debug) println(s"$worker$updatedMind rejects $task from $opponent")
        sender ! Reject(task, workload)
        nbReject +=1
        goto(Responder) using updatedMind
      }

    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a deprecated rejection from $opponent")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind

    case Event(Accept(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a deprecated acceptance from $opponent")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = worker.workload(updatedMind.bundle, cost)
      if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
      sender ! Withdraw(task, workload)
      nbWithdraw +=1
      stay using updatedMind
  }

  /**
    * Or the agent is a proposer
    */
  when(Proposer, stateTimeout = proposalTimeout) {
    case Event(StateTimeout, mind) =>
      if (debug) println(s"$worker$mind 's proposal to ${mind.opponent} about ${mind.task} timeout")
      self ! Start
      goto(Responder) using mind

    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (opponent != mind.opponent || task != mind.task) {
        if (receiveDebug) println(s"$worker$mind receives a deprecated rejection of $task from $opponent in proposer state ")
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        stay using updatedMind
      } else {
        if (receiveDebug) println(s"$worker$mind receives a rejection of $task from $opponent in proposer state")
        var updatedMind = mind.updateBelief(opponent, opponentWorkload)
        updatedMind = updatedMind.updateDelegation(NoWorker, NoTask)
        self ! Start
        goto(Responder) using updatedMind
      }

    case Event(Accept(task, opponentWorkload), mind) =>
      val workload = mind.belief(worker)
      val opponent = directory.workers(sender)
      if (opponent != mind.opponent || task != mind.task) {
        if (receiveDebug) println(s"$worker$mind receives a deprecated acceptance of $task from $opponent in proposer state ")
        val updatedMind = mind.updateBelief(opponent, opponentWorkload)
        if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
        sender ! Withdraw(task, workload)
        nbWithdraw +=1
        stay using updatedMind
      } else {
        if (receiveDebug) println(s"$worker$mind receives an acceptance of $task from $opponent in proposer state ")
        val workload = mind.belief(worker) - cost(worker, task)
        var updatedMind = mind.remove(task)
        updatedMind = updatedMind.updateBelief(worker, workload)
        updatedMind = updatedMind.updateBelief(opponent, opponentWorkload + cost(opponent, task))
        updatedMind = updatedMind.updateDelegation(NoWorker, NoTask)
        if (debug) println(s"$worker$updatedMind confirms $task delegation to $opponent")
        sender ! Confirm(task, workload)
        nbConfirm +=1
        if (receiveDebug) println(s"$worker$updatedMind broadcast its updated workload")
        if (rule == Cmax) {
          broadcastInform(updatedMind.belief(worker))
          nbInform += 1
        }
        self ! Start
        goto(Responder) using updatedMind
      }

    case Event(Propose(task, _), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a proposal of $task from $opponent in proposer state")
      stash
      stay using mind

    case Event(Start, mind) =>
      stash
      stay using mind
  }

  /**
    * Or the agent waits for confirmation
    */
  when(WaitConfirmation) {
    case Event(Confirm(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a confirmation from $opponent about $task")
      var updatedMind = mind.add(task)
      updatedMind = updatedMind.updateBelief(worker,  updatedMind.belief(worker) + cost(worker, task) )
      updatedMind = updatedMind.updateBelief(opponent, updatedMind.belief(opponent) - cost(opponent, task) )
      if (rule == Cmax) {
        broadcastInform(updatedMind.belief(worker))
        nbInform +=1
      }
      self ! Start
      goto(Responder) using updatedMind

    case Event(Withdraw(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a withdraw from $opponent about $task")
      val updatedMind = mind.updateBelief(opponent, mind.belief(opponent))
      self ! Start
      goto(Responder) using updatedMind

    case Event(Reject(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a deprecated rejection from $opponent about $task")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      stay using updatedMind

    case Event(Accept(task, opponentWorkload), mind) =>
      val opponent = directory.workers(sender)
      if (receiveDebug) println(s"$worker$mind receives a deprecated acceptance from $opponent about $task")
      val updatedMind = mind.updateBelief(opponent, opponentWorkload)
      val workload = worker.workload(updatedMind.bundle, cost)
      if (debug) println(s"$worker$mind withdraws $task delegation to $opponent")
      sender ! Withdraw(task, workload)
      nbWithdraw += 1
      stay using updatedMind

    case Event(Propose(task, _), mind) =>
      val opponent = directory.workers(sender)
      if (debug) println(s"$worker$mind receives a proposal of $task from $opponent in proposer state")
      stash
      stay using mind

    case Event(Start, mind) =>
      if (debug) println(s"$worker$mind is triggered in proposer state")
      stash
      stay using mind
  }

  /**
    * Whatever the state is
    **/
  whenUnhandled {
    case Event(Inform(opponent, workload), mind) =>
      if (receiveDebug) println(s"$worker$mind receives an inform from $opponent in state $mind")
      if (rule == Cmax && workload < mind.belief(opponent)){
        if (debug) println(s"$worker is triggered by the information of the workload $workload from $opponent")
        stopped = false
        supervisor ! ReStarted(mind.bundle)
        if (debug)  println(s"$worker$mind restarts in proposer state")
        self ! Start
      }
      val updatedMind = mind.updateBelief(opponent, workload)
      stay using updatedMind

    case Event(Query, mind) =>
      if (stopped) sender ! Finish(nbPropose, nbAccept, nbReject, nbWithdraw, nbConfirm, nbInform)
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
    case Proposer -> Responder =>
      unstashAll()
      if (stateDebug) println (s"$worker moves from proposer to responder state")
    case Responder -> Proposer =>
      if (stateDebug) println (s"$worker moves from responder to proposer state")
  }

  // Finally Triggering it up using initialize, which performs the transition into the initial state and sets up timers (if required).
  initialize()
}