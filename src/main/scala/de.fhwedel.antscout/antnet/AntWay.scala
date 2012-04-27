package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.{ActorRef, Actor}
import antnet.AntNode._
import osm._
import map.{Node, Way}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, override val nodes: Seq[OsmNode], val startNode: ActorRef, val endNode: ActorRef, val length: Double, val maxSpeed: Double) extends Way(id, nodes) with Logger {

  def containsSlice(nodes: Seq[OsmNode]) = {
    this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse)
  }

  def cross(startNode: ActorRef) = if (startNode == this.startNode) (endNode, tripTime) else (startNode, tripTime)

  /**
   * Berechnet den Endknoten des Weges. Dieser ist davon abhängig, welcher Knoten als Startknoten definiert wird.
   *
   * @param startNode Der als Startknoten definierte Knoten.
   * @return Endknoten
   */
  def endNode(startNode: ActorRef): ActorRef = {
    if (startNode == this.startNode)
      endNode
    else
      this.startNode
  }

  def extend(nodes: Seq[OsmNode]): AntWay = {
    if (this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse))
      this
    else {
      require(Set(startNode.id, endNode.id, nodes.head.id, nodes.last.id).size == 3)
      val newNodes = if (endNode.id == nodes.head.id)
        this.nodes ++ nodes.tail
      else if (endNode.id == nodes.last.id)
        this.nodes ++ nodes.reverse.tail
      else if (startNode.id == nodes.head.id)
        this.nodes.reverse ++ nodes.tail
      else // startNode.id == nodes.last.id
        nodes ++ this.nodes.tail
      AntWay(id, newNodes, maxSpeed)
    }
  }

  def isExtendable(node: OsmNode)(implicit nodeToWaysMapping: Map[OsmNode, Iterable[OsmWay]] = OsmMap nodeWaysMapping) = {
    node.isConnection(nodeToWaysMapping)
  }

  override def toString = "#%s #%s - #%s".format(id, startNode.id, endNode.id)

  def tripTime = length / maxSpeed
}

object AntWay extends Logger {

  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) = {
    new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
  }

  def apply(id: String, maxSpeed: Double, nodes: Seq[OsmNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    val startNodes = Actor.registry.actorsFor(nodes.head.id)
    if (startNodes.size > 1)
      warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNodes = Actor.registry.actorsFor(nodes.last.id)
    if (endNodes.size > 1)
      warn("Multiple end node actors for node #%s".format(nodes.head.id))
    new AntWay(id, Seq(), startNodes(0), endNodes(0), length, maxSpeed)
  }
  
  def apply(id: String, nodes: Seq[OsmNode], maxSpeed: Double, oneWay: Boolean = false) = {
    val startNodeId = nodes.head.id
    val endNodeId = nodes.last.id
    val startNode = Actor.registry.actors.find (_.id == startNodeId) getOrElse AntNode(startNodeId)
    val endNode = Actor.registry.actors.find (_.id == endNodeId) getOrElse AntNode(endNodeId)
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    if (oneWay)
      new AntOneWay(id, nodes, startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, nodes, startNode, endNode, length, maxSpeed)
  }

  def apply(id: String, ways: Seq[OsmWay]) = {
    val outerNodes = OsmMap.outerNodes(ways)
    val (startNodeId, endNodeId) = (outerNodes._1.id, outerNodes._2.id)
    val startNode = Actor.registry.actors.find (_.id == startNodeId) getOrElse AntNode(startNodeId)
    val endNode = Actor.registry.actors.find (_.id == endNodeId) getOrElse AntNode(endNodeId)
    val length = ways.map(_.length).sum
    val maxSpeed = ways.map(_.maxSpeed).sum / ways.size
    if (ways.forall(_.isInstanceOf[OsmOneWay]))
      new AntOneWay(id, Seq(), startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
  }

  def apply(osmWay: OsmOneWay, idSuffix: Int, nodes: Seq[OsmNode], antNodes: List[String]) = {
    val startNode = Actor.registry.actorsFor(nodes.head.id)
    if (startNode.size > 1) warn("Multiple start node actors for node #%s".format(nodes.head.id))
    val endNode = Actor.registry.actorsFor(nodes.last.id)
    if (endNode.size > 1) warn("Multiple end node actors for node #%s".format(nodes.head.id))
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntOneWay("%s-%d".format(osmWay.id, idSuffix), Seq(), startNode(0), endNode(0), length, osmWay.maxSpeed)
  }
}
