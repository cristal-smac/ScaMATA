// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import java.util.concurrent.ThreadLocalRandom

import org.scamata.core.{NoTask, Task, Agent}
import org.scamata.solver.{DealStrategy, LF, LCmax, SingleGiftOnly, SocialRule}

import scala.collection.SortedSet
import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * States of the worker agent
  */
sealed trait State

case object Initial extends State

case object Proposer extends State

case object Responder extends State

/**
  * Internal immutable state of mind
  *
  * @param bundle    of the worker
  * @param belief    about the workloads
  * @param responder under consideration
  * @param task      under consideration
  * @param blackList of task/worker
  */
class StateOfMind(val bundle: SortedSet[Task], var belief: Map[Agent, Double], val responder: Agent, val task: Task, var blackList : List[(Task, Agent)])
  extends Product5[SortedSet[Task], Map[Agent, Double], Agent, Task, List[(Task, Agent)]] {

  override def _1: SortedSet[Task] = bundle

  override def _2: Map[Agent, Double] = belief

  override def _3: Agent = responder

  override def _4: Task = task

  override def _5: List[(Task, Agent)] = blackList

  override def toString: String = bundle.mkString("(", ",", ")")

  override def canEqual(that: Any): Boolean = that.isInstanceOf[StateOfMind]

  def initBelief(newBundle: SortedSet[Task], workers: Iterable[Agent]): StateOfMind = {
    workers.foreach { w =>
      belief += w -> 0.0
    }
    new StateOfMind(bundle, belief, responder, task, List[(Task,Agent)]())
  }

  /**
    * Update belief with a new workload
    */
  def updateBelief(worker: Agent, workload: Double): StateOfMind = {
    new StateOfMind(bundle, belief.updated(worker, workload), responder, task, blackList)
  }

  /**
    * Add a new bundle to the current one
    */
  def addBundle(newBundle: SortedSet[Task]): StateOfMind = {
    new StateOfMind(bundle ++ newBundle, belief, responder, task, blackList)
  }

  /**
    * Add a task to the current bundle
    */
  def add(task: Task): StateOfMind = {
    new StateOfMind(bundle + task, belief, responder, task, blackList)
  }

  /**
    * Remove a task from the current bundle
    */
  def remove(task: Task): StateOfMind = {
    new StateOfMind(bundle - task, belief, responder, task, blackList)
  }

  /**
    * Change the task and the responder under consideration
    */
  def changeDelegation(newOpponent: Agent, newTask: Task): StateOfMind = {
    new StateOfMind(bundle, belief, newOpponent, newTask, blackList)
  }

  /**
    * Put a task and a worker agent in the black list
    */
  def barred(t: Task, a: Agent) : StateOfMind = {
    new StateOfMind(bundle, belief, responder, task, (t, a) :: blackList  )
  }

  /**
    * Is a task and a worker agent in the black list ?
    */
  def isBarred(t: Task, a: Agent) : Boolean = blackList.contains(t, a)

  /**
    * Return the belief about the flowtime
    */
  def flowtime(): Double = belief.values.sum
}

/**
  * Abstract class representing a worker agent
  *
  * @param worker   which is embedded
  * @param rule     to optimize
  * @param strategy for negotiating
  */
abstract class WorkerAgent(val worker: Agent, val rule: SocialRule, val strategy: DealStrategy) extends Actor {
  var trace: Boolean = false
  var debug: Boolean = false

  val rnd : ThreadLocalRandom = ThreadLocalRandom.current()

  val deadline: FiniteDuration = 300 nanosecond

  val forgetRate = 10 // rate of drop proposals in Proposer state in [0,100]

  var nbPropose = 0
  var nbCounterPropose = 0
  var nbAccept = 0
  var nbReject = 0
  var nbWithdraw = 0
  var nbConfirmGift = 0
  var nbConfirmSwap = 0
  var nbCancel = 0
  var nbInform = 0

  var solverAgent: ActorRef = context.parent
  var directory: Directory = new Directory()
  var costMatrix: Map[(Agent, Task), Double] = Map[(Agent, Task), Double]()

  /**
    * Return the cost of a task for a worker, eventually 0.0 if NoTask
    */
  def cost(worker: Agent, task: Task): Double = if (task != NoTask) costMatrix(worker, task) else 0.0


  /**
    * Broadcasts workload
    */
  def broadcastInform(workload: Double): Unit = {
    directory.peersActor(worker).foreach(_ ! Inform(worker, workload))
  }

  /**
    * Handles the managing message
    *
    * @param message
    */
  def defaultReceive(message: Message): Any = message match {
    case Stop => context.stop(self)
    case Trace =>
      this.trace = true
  }

  /**
    * Returns true if a task can be delegated according to the beliefs
    *
    * @param task     to take
    * @param provider of the task
    * @param supplier of the task
    */
  def acceptable(task: Task, provider: Agent, supplier: Agent, mind: StateOfMind): Boolean = {
    rule match {
      case LCmax =>
        Math.max(mind.belief(provider), mind.belief(supplier)) >
          Math.max(mind.belief(provider) - cost(provider, task), mind.belief(supplier) + cost(supplier, task))
      case LF =>
        cost(provider, task) > cost(supplier, task)
    }
  }

  /**
    * Returns true if a task and a counterpart can be swapped according to the beliefs
    *
    * @param task        to take
    * @param counterpart to give
    * @param provider    of the task
    * @param supplier    of the task
    */
  def acceptable(task: Task, counterpart: Task, provider: Agent, supplier: Agent, mind: StateOfMind): Boolean = {
    rule match {
      case LCmax =>
        Math.max(mind.belief(provider), mind.belief(supplier)) >
          Math.max(mind.belief(provider) - cost(provider, task) + cost(provider, counterpart),
            mind.belief(supplier) + cost(supplier, task) - cost(supplier, counterpart))
      case LF =>
        cost(provider, task) > cost(supplier, task) &&
          cost(supplier, counterpart) > cost(provider, counterpart)
    }
  }

  /**
    * Returns the best counterpart eventually NoTask wrt the task and the responder according to the beliefs
    */
  def bestCounterpart(task: Task, opponent: Agent, mind: StateOfMind): Task = {
    if (strategy == SingleGiftOnly) return NoTask
    val workload = mind.belief(worker)
    var bestCounterpart: Task = NoTask
    var bestGoal = rule match {
      case LCmax =>
        mind.belief(opponent)
      case LF =>
        0.0
    }
    (mind.bundle + NoTask).foreach { counterpart => // foreach potential single swap
      val swapWorkload = workload + cost(worker, task) - cost(worker, counterpart)
      val swapOpponentWorkload = mind.belief(opponent) - cost(opponent, task) + cost(opponent, counterpart)
      val swapGoal = rule match {
        case LCmax =>
          Math.max(swapWorkload, swapOpponentWorkload)
        case LF =>
          if (counterpart!= NoTask &&
            cost(opponent, task) > cost(worker, task) &&
            cost(worker, counterpart) > cost(opponent, counterpart))
            cost(opponent, task) - cost(worker, task) + cost(worker, counterpart) - cost(opponent, counterpart)
          else if (counterpart!= NoTask &&
            cost(opponent, task) < cost(worker, task))
            cost(opponent, task) - cost(worker, task)
          else 0.0
      }
      if (swapGoal < bestGoal) {
        bestGoal = swapGoal
        bestCounterpart = counterpart
      }
    }
    bestCounterpart
  }


}