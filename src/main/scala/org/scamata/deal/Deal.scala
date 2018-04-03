// Copyright (C) Maxime MORGE 2018
package org.scamata.deal

import org.scamata.core._


/**
  * Class representing a deal
  * @param contractors the agents involved
  * @param bundles task to exchange
  */
class Deal(val contractors: List[Agent], val bundles: List[Set[Task]]) {

}

class Swap(val agent1: Agent, val agent2: Agent, val bundle1: Set[Task], val bundle2: Set[Task])
  extends Deal(List(agent1, agent2), List(bundle1, bundle2))

class Gift(val provider: Agent, val supplier: Agent, val bundle: Set[Task])
  extends Swap(provider, supplier, bundle, Set[Task]())

class SingleGift(provider: Agent, supplier: Agent, val task: Task)
  extends Gift(provider: Agent,supplier, Set[Task](task)) {
  override def toString: String = s"$provider gives $task to $supplier"
}
