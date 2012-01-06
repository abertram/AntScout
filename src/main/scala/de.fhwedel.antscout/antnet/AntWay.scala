package de.fhwedel.antscout
package antnet

import osm.{OsmOneWay, OsmNode}
import net.liftweb.common.Logger
import akka.actor.{ActorRef, Actor}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, val startNode: ActorRef, val endNode: ActorRef, val length: Double, val maxSpeed: Double) extends Actor with Logger {

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case Cross(sn) => if (sn == startNode) self.tryReply(EndNode(endNode, tripTime)) else self.tryReply(EndNode(startNode, tripTime))
    case TravelTimeRequest => self.reply(self -> tripTime)
  }

  override def toString = "#%s #%d - #%d".format(id, startNode.id, endNode.id)

  def tripTime = length / maxSpeed
}

object AntWay extends Logger {
  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) = {
    Actor.actorOf(new AntWay(id, startNode, endNode, length, maxSpeed)).start
  }

  def apply(id: String, maxSpeed: Double, nodes: List[OsmNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    val startNodes = Actor.registry.actorsFor(nodes.head.id)
    if (startNodes.size > 1)
      warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNodes = Actor.registry.actorsFor(nodes.last.id)
    if (endNodes.size > 1)
      warn("Multiple end node actors for node #%s".format(nodes.head.id))
    Actor.actorOf(new AntWay(id, startNodes(0), endNodes(0), length, maxSpeed)).start()
  }

  def apply(osmWay: OsmOneWay, idSuffix: Int, nodes: List[OsmNode], antNodes: List[String]) = {
    val startNode = Actor.registry.actorsFor(nodes.head.id)
    if (startNode.size > 1) warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNode = Actor.registry.actorsFor(nodes.last.id)
    if (endNode.size > 1) warn("Multiple end node actors for node #%s".format(nodes.head.id))
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntOneWay("%s-%d".format(osmWay.id, idSuffix), startNode(0), endNode(0), length, osmWay.maxSpeed)
  }
}

case class Cross(startNode: ActorRef)
case object TravelTimeRequest
