// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Task, Worker}
import akka.actor.{Actor, FSM}
import org.scamata.deal.{Gift, SingleGift}
import org.scamata.solver.{Cmax, Flowtime, SocialRule}

import scala.collection.SortedSet

/**
  * States of the agent
  */
sealed trait State
case object Pause extends State
case object Active extends State
case object Proposer extends State
case object Responder extends State
case object Evaluator extends State


/**
  * Internal immutable state of mind
  * @param bundle
  * @param workers
  */
class StateOfMind(val bundle: SortedSet[Task], val workers: Iterable[Worker], val workload: Map[Worker, Double])
  extends Tuple3[SortedSet[Task], Iterable[Worker], Map[Worker, Double]](bundle, workers, workload)



/**
  * Abstract class representing an agent
  * @param worker
  * @param rule to optimize
  */
abstract class Agent(val worker: Worker, val rule: SocialRule) extends Actor{
  val debug=false

  var supervisor = context.parent
  var directory = new Directory()
  var cost = Map[(Worker, Task), Double]()

  /**
    * Broadcasts workload fo the worker
    */
  def broadcastInform(workload: Double) : Unit = {
    directory.allActors.foreach(_ ! Inform(worker, workload))
  }


  def defaultReceive(message : Message) : Any = message match {
    case Initiate(d, c) =>
      this.directory = d
      this.cost = c
    case Stop => context.stop(self)
  }
}

