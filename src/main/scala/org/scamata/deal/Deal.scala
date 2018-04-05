// Copyright (C) Maxime MORGE 2018
package org.scamata.deal

import org.scamata.core._


/**
  * Class representing a deal
  * @param contractors the agents involved
  * @param bundles task to exchange
  */
class Deal(val contractors: List[Worker], val bundles: List[Set[Task]]) {

}

class Swap(val agent1: Worker, val agent2: Worker, val bundle1: Set[Task], val bundle2: Set[Task])
  extends Deal(List(agent1, agent2), List(bundle1, bundle2))

class Gift(val provider: Worker, val supplier: Worker, val bundle: Set[Task])
  extends Swap(provider, supplier, bundle, Set[Task]())

class SingleGift(provider: Worker, supplier: Worker, val task: Task)
  extends Gift(provider: Worker,supplier, Set[Task](task)) {
  override def toString: String = s"$provider gives $task to $supplier"
}
