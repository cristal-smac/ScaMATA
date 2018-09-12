// Copyright (C) Maxime MORGE 2018
package org.scamata.deal

import org.scamata.core._

/**
  * Class representing a deal
  * @param contractors the peers involved
  * @param bundles task to exchange
  */
class Deal(val contractors: List[Agent], val bundles: List[Set[Task]])

class Swap(worker1: Agent, worker2: Agent, bundle1: Set[Task], bundle2: Set[Task])
  extends Deal(List(worker1, worker2), List(bundle1, bundle2))

class SingleSwap(val worker1: Agent, val worker2: Agent, val task1: Task, val task2: Task)
  extends Swap(worker1, worker2, Set(task1), Set(task2)){
  override def toString: String = s"$worker1 gives $task1 to $worker2 which gives $task2 "
}

class Gift(provider: Agent, supplier: Agent, bundle: Set[Task])
  extends Swap(provider, supplier, bundle, Set[Task]())

class SingleGift(val provider: Agent, val supplier: Agent, val task: Task)
  extends Gift(provider, supplier, Set[Task](task)) {
  override def toString: String = s"$provider gives $task to $supplier"
}
