// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import org.scamata.core.{Worker, Allocation, Task}

import scala.collection.SortedSet

/**
  *  All possible messages between the actors
  */
class Message
case object Trigger extends Message
// Initiate the worker with the directory, the cost matrix and the individual bundle
case class Initiate(directory : Directory, cost : Map[(Worker, Task), Double]) extends Message
case class Give(bundle: SortedSet[Task]) extends Message// Give a bundle
case class Result(allocation : Allocation) extends  Message// The supervisor returns an allocation
case object Stop extends Message// Stop an agnet

case class Inform(worker: Worker, workload: Double) extends Message

case class Propose(task : Task, workload: Double) extends Message// Make a proposal
case class CounterPropose(task : Task, workload: Double) extends Message// Reject a proposal
case class Reject(task : Task, workload: Double) extends Message// Reject a proposal
case class Accept(task : Task, workload: Double) extends Message// Accept a proposal
case class Withdraw(task : Task, workload: Double) extends Message// Withdraw a deal

case class Stopped(bundle: SortedSet[Task]) extends Message// Initiate the supervisor
case class ReStarted(bundle: SortedSet[Task]) extends Message// idem

