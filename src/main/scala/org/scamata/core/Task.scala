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
      case that: Task => that.canEqual(this) && this.name.equals(that.name)
      case _ => false
    }
  def canEqual(a: Any): Boolean = a.isInstanceOf[Task]

  /**
    * Returns 0 if this and that are the same, negative if this < that, and positive otherwise
    */
  def compare(that: Task) : Int = {
    if (this.name == that.name) return 0
    else if (this.name > that.name) return 1
    -1
  }
}

/**
  * The default task
  */
object NoTask extends Task("θ")