// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

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
  * @param bundle
  * @param belief about the workloads
  * @param opponent under consideration
  * @param task under consideration
  */
class StateOfMind(val bundle: SortedSet[Task], var belief: Map[Worker, Double], val opponent : Worker, val task : Task)
  extends Product4[SortedSet[Task], Map[Worker, Double], Worker, Task] {
  override def _1: SortedSet[Task] = bundle
  override def _2: Map[Worker, Double] = belief
  override def _3 : Worker = opponent
  override def _4 : Task = task
  override def canEqual(that: Any): Boolean = that.isInstanceOf[StateOfMind]

  def initBelief(newBundle :  SortedSet[Task], workers : Iterable[Worker]) : StateOfMind = {
    workers.foreach{w =>
      belief += w -> 0.0
    }
    new StateOfMind(bundle, belief, opponent, task)
  }
  /**
    * Update belief with a new workload
    */
  def updateBelief(worker: Worker, workload : Double) : StateOfMind = {
    new StateOfMind(bundle, belief.updated(worker, workload), opponent, task)
  }
  /**
    * Add a new bundle to the current one
    */
  def addBundle(newBundle : SortedSet[Task]) : StateOfMind= {
    new StateOfMind(bundle ++ newBundle, belief, opponent, task)
  }

  /**
    * Add a task to the current bundle
    */
  def add(task : Task) : StateOfMind = {
    new StateOfMind(bundle + task, belief, opponent, task)
  }

  /**
    * Remove a task to the current bundle
    */
  def remove(task : Task) : StateOfMind = {
    new StateOfMind(bundle - task, belief, opponent, task)
  }

  /**
    * Change the task and the opponent under consideration
    */
  def changeDelegation(newOpponent : Worker, newTask : Task) : StateOfMind= {
    new StateOfMind(bundle, belief, newOpponent, newTask)
  }

  /**
    * Return the belief about the flowtime
    */
  def flowtime() : Double = belief.values.sum

  override def toString: String = bundle.mkString("(",",",")")
}


/**
  * Abstract class representing an worker
  * @param worker which is embedded
  * @param rule to optimize
  */
abstract class WorkerAgent(val worker: Worker, val rule: SocialRule) extends Actor{
  val trace = false
  val debug = false

  val deadline = 300 nanosecond

  var nbPropose = 0
  var nbAccept = 0
  var nbReject = 0
  var nbWithdraw = 0
  var nbConfirm = 0
  var nbCancel = 0
  var nbInform = 0

  var solverAgent : ActorRef = context.parent
  var directory : Directory = new Directory()
  var cost : Map[(Worker, Task), Double]= Map[(Worker, Task), Double]()

  /**
    * Broadcasts workload
    */
  def broadcastInform(workload: Double) : Unit = {
    directory.peersActor(worker).foreach(_ ! Inform(worker, workload))
  }

  /**
    * Handles the stop message
    * @param message
    */
  def defaultReceive(message : Message) : Any = message match {
    case Stop => context.stop(self) // Stop the actor
  }

  /**
    * Returns true if a task can be delegated
    * @param task to delegate
    * @param provider
    * @param supplier
    */
  def acceptable(task : Task, provider : Worker, supplier : Worker, mind : StateOfMind) : Boolean = {
    rule match {
      case Cmax => // The local Cmax must strictly decrease
        Math.max( mind.belief(provider), mind.belief(supplier)) > Math.max( mind.belief(provider)-cost(provider, task),  mind.belief(supplier) + cost(supplier, task))
      case Flowtime => // The local flowtime  must strictly decrease
        cost(provider, task) > cost(supplier, task)
    }
  }
}
