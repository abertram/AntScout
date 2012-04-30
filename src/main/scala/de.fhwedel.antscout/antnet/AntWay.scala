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

  override def toString = "#%s #%s - #%s".format(id, startNode.id, endNode.id)

  def tripTime = length / maxSpeed
}

object AntWay extends Logger {

  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) = {
    new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
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
}
