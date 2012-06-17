package de.fhwedel.antscout
package antnet

import akka.agent.Agent
import net.liftweb.common.Logger
import net.liftweb.json.JsonDSL._
import osm._
import map.Way

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, override val nodes: Seq[OsmNode], val startNode: AntNode, val endNode: AntNode, val length: Double, originalMaxSpeed: Double) extends Way(id, nodes) with Logger {

  private val _maxSpeed = Agent(originalMaxSpeed)(AntScout.system)

  /**
   * Methode, mit der eine Ameise den Weg kreuzen kann.
   *
   * @param startNode Knoten, an dem der Weg betreten wird.
   * @return Tuple, der aus dem Endknoten des Weges und der benötigten Reisezeit besteht.
   */
  def cross(startNode: AntNode) = (endNode(startNode), tripTime)

  /**
   * Berechnet den Endknoten des Weges. Dieser ist davon abhängig, welcher Knoten als Startknoten definiert wird.
   *
   * @param startNode Der als Startknoten definierte Knoten.
   * @return Endknoten
   */
  def endNode(startNode: AntNode): AntNode = {
    if (startNode == this.startNode)
      endNode
    else
      this.startNode
  }

  def maxSpeed = _maxSpeed()

  def maxSpeed_=(value: Double) = _maxSpeed send value

  override def toJson = {
    super.toJson ~
    ("length" -> length.round) ~
    ("maxSpeed" -> maxSpeed) ~
    ("tripTime" -> tripTime)
  }

  override def toString = "#%s #%s - #%s".format(id, startNode.id, endNode.id)

  def tripTime = length / maxSpeed

  def update(update: AntWay.Update) = {
    maxSpeed = update.maxSpeed
  }
}

object AntWay extends Logger {

  case class Update(maxSpeed: Double)

  def apply(id: String, startNode: AntNode, endNode: AntNode, length: Double, maxSpeed: Double) = {
    new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
  }

  def apply(id: String, nodes: Seq[OsmNode], maxSpeed: Double, oneWay: Boolean = false) = {
    val startNodeId = nodes.head.id
    val endNodeId = nodes.last.id
    val startNode = AntMap.nodes.find (_.id == startNodeId) getOrElse AntNode(startNodeId)
    val endNode = AntMap.nodes.find (_.id == endNodeId) getOrElse AntNode(endNodeId)
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    if (oneWay)
      new AntOneWay(id, nodes, startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, nodes, startNode, endNode, length, maxSpeed)
  }
}
