// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Worker, Allocation, Task}

import scala.collection.SortedSet

/**
  *  All possible messages between the actors
  */
class Message

// Managing messages
case object Debug extends  Message
// Start the reallocation
case class Start(allocation: Allocation) extends Message
// Initiate the worker with the bundle the directory and the cost matrix
case class Initiate(bundle: SortedSet[Task], directory : Directory, cost : Map[(Worker, Task), Double]) extends Message
// The worker agent is ready to start
case object Ready extends Message
// Worker agent informs the solver agent it is deseperated
case class Stopped(bundle: SortedSet[Task]) extends Message
// Worker agent informs the solver agent it is busy
case class Activated(bundle: SortedSet[Task]) extends Message
// Query the statistics
case object Query extends Message
// Report statistics
case class Finish(nbPropose : Int, nbAccept : Int, nbReject : Int, nbWithdraw : Int, nbConfirm : Int, nbCancel : Int,  nbInform : Int) extends Message
// The solver agent returns an allocation and the statistics
case class Outcome(allocation : Allocation, nbPropose : Int, nbAccept : Int, nbReject : Int, nbWithdraw : Int, nbConfirm : Int, nbCancel : Int, nbInform : Int) extends  Message
// Stop an agent
case object Stop extends Message


// Negotiation messages
case object Trigger extends Message // Trigger a negotiation as initiator if possible
case class Propose(task : Task, workload: Double) extends Message// Make a proposal
case class CounterPropose(task : Task, counterpart : Task, workload: Double, id : Int) extends Message // Make a counter-proposal
case class Reject(task : Task, workload: Double) extends Message // Reject a proposal
case class Accept(task : Task, workload: Double) extends Message // Accept a proposal
case class Confirm(task : Task, workload: Double) extends Message // Confirm a deal
case class Withdraw(task : Task, workload: Double) extends Message // Withdraw a deal
case class Cancel(task : Task, workload: Double) extends Message // Cancel a deal
case class Inform(worker: Worker, workload: Double) extends Message // Inform the peer about the workload


