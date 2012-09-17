package de.fhwedel.antscout
package antnet

import akka.agent.Agent
import akka.util.duration._
import akka.util.Timeout
import net.liftweb.common.Logger
import net.liftweb.json.JsonDSL._
import osm._
import map.{Node, Way}
import akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, override val nodes: Seq[Node], val startNode: ActorRef, val endNode: ActorRef,
  val length: Double, originalMaxSpeed: Double) extends Way(id, nodes) with Logger {

  implicit val system = AntScout.system
  implicit val timeout = Timeout(5 seconds)

  private val _maxSpeed = Agent(originalMaxSpeed)

  /**
   * Methode, mit der eine Ameise den Weg kreuzen kann.
   *
   * @param startNode Knoten, an dem der Weg betreten wird.
   * @return Tuple, der aus dem Endknoten des Weges und der benötigten Reisezeit besteht.
   */
  def cross(startNode: ActorRef) = (endNode(startNode), tripTime)

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

  def endNode(startNode: Node) = {
    if (startNode == nodes.head)
      nodes.last
    else
      nodes.head
  }

  def maxSpeed(implicit await: Boolean = false) = {
    if (await)
      _maxSpeed.await
    else
      _maxSpeed.get
  }

  def maxSpeed_=(value: Double) = _maxSpeed.send(value)

  def startAndEndNodes = Set(startNode, endNode)

  override def toJson = {
    super.toJson ~
    ("length" -> length.round) ~
    ("maxSpeed" -> maxSpeed) ~
    ("tripTime" -> tripTime)
  }

  override def toString = "#%s #%s - #%s".format(id, AntNode.nodeId(startNode), AntNode.nodeId(endNode))

  def tripTime = length / maxSpeed

  def update(update: AntWay.Update) = {
    maxSpeed = update.maxSpeed
  }
}

object AntWay extends Logger {

  case class Update(maxSpeed: Double)

  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) = {
    new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
  }

  def apply(id: String, nodes: Seq[OsmNode], maxSpeed: Double, oneWay: Boolean = false) = {
    val startNodeId = nodes.head.id
    val endNodeId = nodes.last.id
    val startNode = AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName,
      startNodeId))
    val endNode = AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, endNodeId))
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    if (oneWay)
      new AntOneWay(id, nodes, startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, nodes, startNode, endNode, length, maxSpeed)
  }
}
