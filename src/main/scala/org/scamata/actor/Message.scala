// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Worker, Allocation, Task}

import scala.collection.SortedSet

/**
  *  All possible messages between the actors
  */
class Message
case object Start extends Message
// Initiate the worker with the bundle the directory and the cost matrix
case class Initiate(bundle: SortedSet[Task], directory : Directory, cost : Map[(Worker, Task), Double]) extends Message
case object Ready extends Message
case class Outcome(allocation : Allocation, nbDeal : Int) extends  Message// The supervisor returns an allocation
case object Stop extends Message// Stop an agent
case class Finish(nbDeal : Int) extends Message// Provide the number of delegation

case class Inform(worker: Worker, workload: Double) extends Message

case class Propose(task : Task, workload: Double) extends Message// Make a proposal
case class CounterPropose(task : Task, counterpart : Task, workload: Double) extends Message// Make a counter-proposal
case class Reject(task : Task, workload: Double) extends Message// Reject a proposal
case class Accept(task : Task, workload: Double) extends Message// Accept a proposal
case class Confirm(task : Task, workload: Double) extends Message// Confirm a deal
case class Withdraw(task : Task, workload: Double) extends Message// Withdraw a deal


case object Query extends Message// Initiate the supervisor
case class Stopped(bundle: SortedSet[Task]) extends Message// Initiate the supervisor
case class ReStarted(bundle: SortedSet[Task]) extends Message// idem

