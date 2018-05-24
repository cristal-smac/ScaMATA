// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.collection.SortedSet
import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/**
  * States of the agent
  */
sealed trait State
case object Proposer extends State
case object Responder extends State
case object WaitConfirmation extends State

/**
  * Internal immutable state of mind
  * @param bundle
  * @param belief about the workloads
  * @param conversationId
  */
class StateOfMind(val bundle: SortedSet[Task], var belief: Map[Worker, Double], val conversationId : Int)
  extends Product3[SortedSet[Task], Map[Worker, Double], Int] {
  override def _1: SortedSet[Task] = bundle
  override def _2: Map[Worker, Double] = belief
  override def _3 : Int = conversationId
  override def canEqual(that: Any): Boolean = that.isInstanceOf[StateOfMind]

  /**
    * Init the belief with some workers with no workload* @return
    */
  def initBelief(workers : Iterable[Worker]) : StateOfMind = {
    workers.foreach{w =>
      belief += w -> 0.0
    }
    new StateOfMind(bundle, belief, conversationId)
  }
  /**
    * Update belief with a new workload
    */
  def updateBelief(worker: Worker, workload : Double) : StateOfMind = {
    new StateOfMind(bundle, belief.updated(worker, workload), conversationId)
  }
  /**
    * Adding to the bundle
    * @param tasks to be added
    */
  def addBundle(tasks : SortedSet[Task]) : StateOfMind= {
    new StateOfMind(bundle ++ tasks, belief, conversationId)
  }

  /**
    * Add a task to the bundler
    */
  def add(task : Task) : StateOfMind = {
    new StateOfMind(bundle + task, belief, conversationId)
  }

  /**
    * Remove a task from the bundle
    */
  def remove(task : Task) : StateOfMind = {
    new StateOfMind(bundle - task, belief, conversationId)
  }


  /**
    * Update the potential supplier and the potential task to supply
    */
  def updateConversationId(conversationId : Int) : StateOfMind= {
    new StateOfMind(bundle, belief, conversationId)
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
abstract class Agent(val worker: Worker, val rule: SocialRule) extends Actor{
  val debug = false
  val stateDebug = false

  var nbPropose = 0
  var nbAccept = 0
  var nbReject = 0
  var nbWithdraw = 0
  var nbConfirm = 0
  var nbCancel = 0
  var nbInform = 0

  var supervisor : ActorRef = context.parent
  var directory : Directory = new Directory()
  var cost : Map[(Worker, Task), Double]= Map[(Worker, Task), Double]()

  val rnd = new scala.util.Random(worker.name.hashCode)
  def randomTimeout : FiniteDuration = Random.nextInt(10000000) nanosecond

  /**
    * Broadcasts workload
    */
  def broadcastInform(workload: Double) : Unit = {
    directory.peersActor(worker).foreach(_ ! Inform(worker, workload))
  }

  /**
    * Handle stop messages
    */
  def defaultReceive(message : Message) : Any = message match {
    case Stop =>
      context.stop(self) // Stop the actor
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

