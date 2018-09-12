// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.ActorRef
import org.scamata.core.Agent

/**
  * Class representing an index of the names and addresses of peers
  */
class Directory {
  var adr = Map[Agent, ActorRef]()//Agents' references
  var workers = Map[ActorRef, Agent]()// Actors' worker

  override def toString: String = allWorkers().mkString("[",", ","]")

  /**
    * Add to the directory
    * @param worker
    * @param ref
    */
  def add(worker: Agent, ref: ActorRef) : Unit = {
    if ( ! adr.keySet.contains(worker) &&  ! workers.keySet.contains(ref)) {
      adr += (worker -> ref)
      workers += (ref -> worker)
    }
    else throw new RuntimeException(s"$worker and/or $ref already in the directory")
  }

  def allActors() : Iterable[ActorRef]  = adr.values
  def allWorkers() : Iterable[Agent]  = workers.values
  def peers(worker: Agent) : Set[Agent] = allWorkers().filterNot(_ ==worker).toSet
  def peersActor(worker: Agent) :  Iterable[ActorRef] = peers(worker: Agent).map(w => adr(w))

}
