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
  val memory = new Stack[(ActorRef, ActorRef)]

  def memorizeVisitedNodeAntWay(node: ActorRef, way: ActorRef) {
    memory.push(node -> way)
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

  def removeCircle(node: ActorRef) {
    trace("Removing circle of #%s".format(node id))
    memory.dropWhile(_._1 != node).pop()
  }

  def selectWay(propabilities: Map[ActorRef, Double]) = {
    trace("selectWay")
    debug("Visited ways: %s".format(memory.map(_._2.id).mkString(",")))
    val notVisitedWays = propabilities.filter { case (w, p) => memory.groupBy(_._2).keySet.contains(w) }
    debug("Not visited ways: %s".format(notVisitedWays.map(_._1.id).mkString(",")))
    currentWay = if (!notVisitedWays.isEmpty) notVisitedWays.maxBy(_._2)._1 else propabilities.maxBy(_._2)._1
    debug("Selected way: #%s".format(currentWay id))
    currentWay
  }

  def visitNode(node: ActorRef) {
    trace("Visiting node #%s".format(node id))
    currentNode = node
    if (node != destinationNode) {
      if (memory.groupBy(_._1).keySet.contains(node)) {
        debug("Circle detected")
        removeCircle(node)
      }
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