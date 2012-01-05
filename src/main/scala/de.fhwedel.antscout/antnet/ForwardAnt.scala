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
    case EndNode(n) => visitNode(n)
    case Propabilities(ps) => selectWay(ps) ! Cross
    case m: Any => warn("Unknown message: %s".format(m))
  }

  def selectWay(propabilities: Map[ActorRef, Double]) = {
    propabilities.maxBy(_._2)._1
  }

  def visitNode(node: ActorRef) = {
    debug("Visiting node %s".format(node id))
    if (node != destinationNode) {
      node ! Enter(destinationNode)
    } else {
      info("Destination reached")
    }
  }
}

object ForwardAnt {
  def apply(sourceNode: ActorRef, destinationNode: ActorRef) = new ForwardAnt(sourceNode, destinationNode)
}

case class EndNode(node: ActorRef)
case class Propabilities(propabilities: Map[ActorRef, Double])