// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Agent, Allocation, Task}

import scala.collection.SortedSet

/**
  *  All possible messages between the actors
  */
class Message

// Managing messages
case object Trace extends  Message
// Start the reallocation
case class Start(allocation: Allocation) extends Message
// Initiate the worker with the bundle the directory and the cost matrix
case class Initiate(bundle: SortedSet[Task], directory : Directory, cost : Map[(Agent, Task), Double]) extends Message
// The worker agent is ready to start
case object Ready extends Message
// Agent agent informs the solver agent it is deseperated
case class Stopped(bundle: SortedSet[Task]) extends Message
// Agent agent informs the solver agent it is busy
case class Activated(bundle: SortedSet[Task]) extends Message
// Query the statistics
case object Query extends Message
// Report statistics
case class Finish(nbPropose : Int, nbCounterPropose : Int, nbAccept : Int, nbReject : Int, nbWithdraw : Int, nbConfirmGift : Int, nbConfirmSwap : Int, nbCancel : Int,  nbInform : Int) extends Message
// The solver agent returns an allocation and the statistics
case class Outcome(allocation : Allocation, nbPropose : Int, nbCounterPropose : Int,nbAccept : Int, nbReject : Int, nbWithdraw : Int, nbConfirmGift : Int, nbConfirmSwap : Int, nbCancel : Int, nbInform : Int) extends  Message
// Stop an agent
case object Stop extends Message


// Negotiation messages
case object Trigger extends Message // Trigger a negotiation as initiator if possible
case class Propose(task : Task, counterpart : Task, workload: Double) extends Message // Make a counter-proposal
case class Reject(task : Task, countpart : Task, workload: Double) extends Message // Reject a proposal
case class Accept(task : Task, countpart : Task, workload: Double) extends Message // Accept a proposal
case class Confirm(task : Task, countpart : Task, workload: Double) extends Message // Confirm a deal
case class Withdraw(task : Task, countpart : Task, workload: Double) extends Message // Withdraw a deal
case class Inform(worker: Agent, workload: Double) extends Message // Inform the peer about the workload


