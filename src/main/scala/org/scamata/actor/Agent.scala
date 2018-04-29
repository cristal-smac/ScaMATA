// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.collection.SortedSet
import akka.actor.{Actor, ActorRef}

/**
  * States of the agent
  */
sealed trait State
case object Proposer extends State
case object Responder extends State

/**
  * Internal immutable state of mind
  * @param bundle
  * @param belief about the workloads
  */
class StateOfMind(val bundle: SortedSet[Task], var belief: Map[Worker, Double])
  extends Product2[SortedSet[Task], Map[Worker, Double]] {
  override def _1: SortedSet[Task] = bundle
  override def _2: Map[Worker, Double] = belief
  override def canEqual(that: Any): Boolean = that.isInstanceOf[StateOfMind]

  /**
    * Update belief with a new workload
    */
  def updateBelief(worker: Worker, workload : Double) : StateOfMind= {
    new StateOfMind(bundle, belief.updated(worker, workload))
  }
  /**
    * Update bundle
    */
  def updateBundle(newBundle : SortedSet[Task]) : StateOfMind= {
    new StateOfMind(newBundle, belief)
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
  val extraDebug = false

  var supervisor : ActorRef = context.parent
  var directory : Directory = new Directory()
  var cost : Map[(Worker, Task), Double]= Map[(Worker, Task), Double]()

  /**
    * Broadcasts workload
    */
  def broadcastInform(workload: Double) : Unit = {
    if (debug) println(s"$worker broadcast its workload: $workload")
    directory.peersActor(worker).foreach(_ ! Inform(worker, workload))
  }

  /**
    * Handles management messages
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

