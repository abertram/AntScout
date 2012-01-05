package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.{ActorRef, Actor}
import collection.mutable.Stack

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 25.12.11
 * Time: 15:21
 */

class ForwardAnt(val sourceNode: ActorRef, val destinationNode: ActorRef) extends Actor with Logger {
  
  var currentNode: ActorRef = _
  var currentWay: ActorRef = _
  val visitedNodesAndWays = new Stack[(ActorRef, ActorRef)]

  def memorizeVisitedNodeAntWay(node: ActorRef, way: ActorRef) {
    visitedNodesAndWays.push(node -> way)
  }

  override def preStart() {
    if (sourceNode == destinationNode) {
      warn("Source node equals destination node, exit!")
      self.stop()
    } else {
      visitNode(sourceNode)
    }
  }

  protected def receive = {
    case EndNode(n) => {
      memorizeVisitedNodeAntWay(currentNode, currentWay)
      visitNode(n)
    }
    case Propabilities(ps) => selectWay(ps) ! Cross(currentNode)
    case m: Any => warn("Unknown message: %s".format(m))
  }

  def selectWay(propabilities: Map[ActorRef, Double]) = {
    val notVisitedWays = propabilities.filter { case (w, p) => visitedNodesAndWays.groupBy(_._2).keySet.contains(w) }
    currentWay = if (!notVisitedWays.isEmpty) notVisitedWays.maxBy(_._2)._1 else propabilities.maxBy(_._2)._1
    currentWay
  }

  def visitNode(node: ActorRef) = {
    currentNode = node
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