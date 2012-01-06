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
  val memory = AntMemory()

  def launchBackwardAnt() {
    BackwardAnt(sourceNode, destinationNode, memory)
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
//      debug("EndNode(%s)".format(n id))
      memory.memorize(currentNode, currentWay)
      visitNode(n)
    }
    case Propabilities(ps) => selectWay(ps) ! Cross(currentNode)
    case m: Any => warn("Unknown message: %s".format(m))
  }

  def selectWay(propabilities: Map[ActorRef, Double]) = {
//    debug("Memory: %s".format(memory.items))
    val notVisitedWays = propabilities.filter { case (w, p) => !memory.containsWay(w) }
//    debug("Not visited ways: %s".format(notVisitedWays.map(_._1.id).mkString(", ")))
    currentWay = if (!notVisitedWays.isEmpty) notVisitedWays.maxBy(_._2)._1 else propabilities.maxBy(_._2)._1
//    debug("Selected way: #%s".format(currentWay id))
//    Thread.sleep(30000)
    currentWay
  }

  def visitNode(node: ActorRef) {
//    debug("Visiting node #%s".format(node id))
    currentNode = node
    if (node != destinationNode) {
      if (memory.containsNode(node)) {
//        debug("Circle detected")
        memory.removeCircle(node)
      }
      node ! Enter(destinationNode)
    } else {
      info("Destination reached")
      launchBackwardAnt()
      self.stop()
    }
  }
}

object ForwardAnt {
  def apply(sourceNode: ActorRef, destinationNode: ActorRef) = new ForwardAnt(sourceNode, destinationNode)
}

case class EndNode(node: ActorRef)
case class Propabilities(propabilities: Map[ActorRef, Double])
