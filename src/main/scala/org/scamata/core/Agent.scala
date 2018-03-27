// Copyright (C) Maxime MORGE 2018
package org.scamata.core

/**
  * Class representing an agent
  * @param name of the agent
  */
class Agent(val name : String) extends Ordered[Agent]{

  override def toString: String = name

  override def equals(that: Any): Boolean =
    that match {
      case that: Agent => that.canEqual(this) && this.name.equals(that.name)
      case _ => false
    }
  def canEqual(a: Any) : Boolean = a.isInstanceOf[Agent]

  /**
    * Returns 0 if the same negative if this < that, positive if this > that
    */
  def compare(that: Agent) : Int = {
    if (this.name.equals(that.name)) return 0
    else if (this.name > that.name) return 1
    -1
  }


}

