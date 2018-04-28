// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import akka.actor.{Actor, ActorRef}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.collection.SortedSet

/**
  * States of the worker
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
  extends Tuple2[SortedSet[Task], Map[Worker, Double]](bundle, belief){

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

}


/**
  * Abstract class representing an worker
  * @param worker which is embedded
  * @param rule to optimize
  */
abstract class Agent(val worker: Worker, val rule: SocialRule) extends Actor{
  val debug = false

  var supervisor : ActorRef = context.parent
  var directory : Directory = new Directory()
  var cost : Map[(Worker, Task), Double]= Map[(Worker, Task), Double]()

  /**
    * Broadcasts workload
    */
  def broadcastInform(workload: Double) : Unit = {
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

