package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import collection.mutable.Stack
import akka.actor.{ActorRef, Actor}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 25.12.11
 * Time: 15:21
 */

class ForwardAnt(val sourceNode: ActorRef, val destinationNode: ActorRef) extends Actor with Logger {
  
  override def preStart() {
    if (sourceNode == destinationNode) {
      warn("Source node equals destination node, exit!")
      self.stop()
    } else {
      visitNode(sourceNode)
    }
  }

  protected def receive = {
    case m: Any => warn("Unknown message: %s".format(m))
  }

  def visitNode(node: ActorRef) = {
    if (node != destinationNode) {

    } else {
      info("Destination reached")
    }
  }
}

object ForwardAnt {
  def apply(sourceNode: ActorRef, destinationNode: ActorRef) = new ForwardAnt(sourceNode, destinationNode)
}
