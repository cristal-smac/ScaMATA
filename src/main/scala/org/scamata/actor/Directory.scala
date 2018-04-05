// Copyright (C) Maxime MORGE 2018
package org.scamata.actor

import akka.actor.ActorRef
import org.scamata.core.Worker

/**
  * Class representing an index of the names and addresses of agents
  */
class Directory {
  var adr = Map[Worker, ActorRef]()//Agents' references
  var name = Map[ActorRef, Worker]()// Actors' worker

  /**
    * Add to the directory
    * @param agent
    * @param ref
    */
  def add(agent: Worker, ref: ActorRef) : Unit = {
    if ( ! adr.keySet.contains(agent) &&  ! name.keySet.contains(ref)) {
      adr += (agent -> ref)
      name += (ref -> agent)
    }
    else throw new RuntimeException(s"$agent and/or $ref already in the directory")
  }

  def allActors() : Iterable[ActorRef]  = adr.values
  def allWorkers() : Iterable[Worker]  = name.values
}
