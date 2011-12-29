package de.fhwedel.antscout
package antnet

import osm.{OsmOneWay, OsmWay, OsmNode}
import net.liftweb.common.Logger
import akka.actor.{ActorRef, Actor}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, val startNode: ActorRef, val endNode: ActorRef, val length: Double) extends Actor {

  override def preStart() {
    self.id = id
  }

  protected def receive = null

  override def toString = "#%s #%d - #%d".format(id, startNode.id, endNode.id)
}

object AntWay extends Logger {
  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double) = {
    Actor.actorOf(new AntWay(id, startNode, endNode, length)).start
  }

  def apply(id: String, nodes: List[OsmNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    val startNodes = Actor.registry.actorsFor(nodes.head.id)
    if (startNodes.size > 1)
      warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNodes = Actor.registry.actorsFor(nodes.last.id)
    if (endNodes.size > 1)
      warn("Multiple end node actors for node #%s".format(nodes.head.id))
    Actor.actorOf(new AntWay(id, startNodes(0), endNodes(0), length)).start
  }
//  def apply(id: Int, startNode: AntNode, endNode: AntNode) = new AntWay(id.toString, startNode, endNode, 0.0)

//  def apply(id: String, startNode: AntNode, endNode: AntNode) = new AntWay(id, startNode, endNode, 0.0)

//  def apply(osmWay: OsmWay, idSuffix: Int, nodes: List[OsmNode], antNodes: List[String]) = {
//    val startNode = Actor.registry.actorsFor(nodes.head.id)
//    if (startNode.size > 1) warn("Multiple start node actors for node #%s".format(nodes.head.id))
//    val endNode = Actor.registry.actorsFor(nodes.last.id)
//    if (endNode.size > 1) warn("Multiple end node actors for node #%s".format(nodes.head.id))
//    (startNode.size, endNode.size) match {
//      case (0, _) =>
//        // warn("Way %s, invalid start node %d".format(osmWay.id, nodes.head.id))
//        None
//      case (_, 0) =>
//        // warn("Way %s, invalid end node %d".format(osmWay.id, nodes.last.id))
//        None
//      case _ =>
//        val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
//        Some(AntWay("%s-%d".format(osmWay.id, idSuffix), startNode(0), endNode(0), length))
//    }
//  }

  def apply(osmWay: OsmOneWay, idSuffix: Int, nodes: List[OsmNode], antNodes: List[String]) = {
    val startNode = Actor.registry.actorsFor(nodes.head.id)
    if (startNode.size > 1) warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNode = Actor.registry.actorsFor(nodes.last.id)
    if (endNode.size > 1) warn("Multiple end node actors for node #%s".format(nodes.head.id))
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntOneWay("%s-%d".format(osmWay.id, idSuffix), startNode(0), endNode(0), length)
  }
}
