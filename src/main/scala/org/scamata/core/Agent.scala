// Copyright (C) Maxime MORGE 2018
package org.scamata.core

import scala.collection.SortedSet

/**
  * Class representing an worker
  * @param name of the worker
  */
class Agent(val name : String) extends Ordered[Agent]{

  override def toString: String = name

  override def equals(that: Any): Boolean =
    that match {
      case that: Agent => that.canEqual(this) && this.name == that.name
      case _ => false
    }
  def canEqual(a: Any) : Boolean = a.isInstanceOf[Agent]

  /**
    * Returns 0 if this an that are the same, negative if this < that, and positive otherwise
    */
  def compare(that: Agent) : Int = {
    if (this.name == that.name) return 0
    else if (this.name > that.name) return 1
    -1
  }

  /**
    * Returns the workload of the worker with
    * @param bundle allocated to the worker
    * @param cost matrix
    */
  def workload(bundle: SortedSet[Task], cost : Map[(Agent, Task), Double]) : Double = {
    bundle.foldLeft(0.0)((acc : Double, t : Task) => acc + cost(this, t))
  }
}

/**
  * The default worker
  */
object NoAgent extends Agent("NoAgent")
