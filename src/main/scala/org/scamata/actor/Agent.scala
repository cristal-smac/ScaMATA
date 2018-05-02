// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.collection.SortedSet
import akka.actor.{Actor, ActorRef}

import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


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
  * @param opponent which is under consideration
  * @param task to supply
  */
class StateOfMind(val bundle: SortedSet[Task], var belief: Map[Worker, Double], val opponent : Worker, val task : Task)
  extends Product4[SortedSet[Task], Map[Worker, Double], Worker, Task] {
  override def _1: SortedSet[Task] = bundle
  override def _2: Map[Worker, Double] = belief
  override def _3 : Worker = opponent
  override def _4 : Task = task
  override def canEqual(that: Any): Boolean = that.isInstanceOf[StateOfMind]

  /**
    * Init the belief with some workers with no workload* @return
    */
  def initBelief(workers : Iterable[Worker]) : StateOfMind = {
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
    * Update bundle by adding a new bundle
    */
  def updateBundle(newBundle : SortedSet[Task]) : StateOfMind= {
    new StateOfMind(bundle ++ newBundle, belief, opponent, task)
  }

  /**
    * Add a task to the bundler
    */
  def add(task : Task) : StateOfMind = {
    new StateOfMind(bundle + task, belief, opponent, task)
  }

  /**
    * Remove a task from the bundle
    */
  def remove(task : Task) : StateOfMind = {
    new StateOfMind(bundle - task, belief, opponent, task)
  }


  /**
    * Update the potential supplier and the potential task to supply
    */
  def updateDelegation(newOpponent : Worker, newTask : Task) : StateOfMind= {
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
abstract class Agent(val worker: Worker, val rule: SocialRule) extends Actor{
  val debug = false
  val receiveDebug = false
  val stateDebug = false

  var supervisor : ActorRef = context.parent
  var directory : Directory = new Directory()
  var cost : Map[(Worker, Task), Double]= Map[(Worker, Task), Double]()

  val proposalTimeout = 300 nanosecond

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

