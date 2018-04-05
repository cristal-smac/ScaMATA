// Copyright (C) Maxime MORGE 2018
package org.scamata.core

/**
  * Class representing a task
  * @param name of the task
  */
class Task(val name : String) extends Ordered[Task]{

  override def toString: String = name

  override def equals(that: Any): Boolean =
    that match {
      case that: Worker => that.canEqual(this) && this.name.equals(that.name)
      case _ => false
    }
  def canEqual(a: Any): Boolean = a.isInstanceOf[Task]

  /**
    * Returns 0 if the same negative if this < that, positive if this > that
    */
  def compare(that: Task) : Int = {
    if (this.name == that.name) return 0
    else if (this.name > that.name) return 1
    -1
  }

}
