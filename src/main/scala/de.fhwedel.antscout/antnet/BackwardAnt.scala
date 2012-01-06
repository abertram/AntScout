package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import collection.mutable.Stack

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 18:55
 */

class BackwardAnt(source: ActorRef, destination: ActorRef, memory: AntMemory) {

  memory.items.foreach {
    case AntMemoryItem(node, way) => node ! UpdateDataStructures(destination, way)
  }
}

object BackwardAnt {

  def apply(source: ActorRef, destination: ActorRef, memory: AntMemory) = new BackwardAnt(source, destination, memory)
}