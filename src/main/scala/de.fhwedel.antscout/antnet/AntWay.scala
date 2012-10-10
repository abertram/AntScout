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
import net.liftweb.json.JsonAST.JArray

/**
 * Repräsentiert einen Weg für den AntNet-Algorithmus.
 *
 * @param id Eindeutige Id.
 * @param nodes Knoten, aus denen der Weg besteht.
 * @param startNode Aktor, der den Start-Knoten repräsentiert.
 * @param endNode Aktor, der den End-Knoten repräsentiert.
 * @param length Weg-Länge in Metern.
 * @param originalMaxSpeed Maximal erlaubte Geschwindigkeit im Metern pro Sekunde.
 */
class AntWay(id: String, override val nodes: Seq[Node], val startNode: ActorRef, val endNode: ActorRef,
  val length: Double, originalMaxSpeed: Double) extends Way(id, nodes) with Logger {

  implicit val timeout = Timeout(5 seconds)

  /**
   * Maximal erlaubte Geschwindigkeit in Metern pro Sekunde.
   */
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

  /**
   * Berechnet den Endknoten des Weges. Dieser ist davon abhängig, welcher Knoten als Startknoten definiert wird.
   *
   * @param startNode Der als Startknoten definierte Knoten.
   * @return Endknoten
   */
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

  /**
   * Erzeugt eine Json-Repräsentation des Weges.
   *
   * @return Json-Repräsentation des Weges.
   */
  override def toJson = {
    super.toJson ~
    // Länge in Metern
    ("length" -> "%.4f".format(length)) ~
    // Länge in weiteren Einheiten
    ("lengths" ->
      JArray(List(("unit" -> "km") ~
      ("value" -> "%.4f".format(length / 1000))))) ~
    // maximal erlaubte Geschwindigkeit
    ("maxSpeed" -> "%.4f".format(maxSpeed)) ~
    // maximal erlaubte Geschwindigkeit in weiteren Einheiten
    ("maxSpeeds" ->
      JArray(List(("unit" -> "km/h") ~
      ("value" -> "%.4f".format(maxSpeed * 3.6))))) ~
    // Reisezeit
    ("tripTime" -> "%.4f".format(tripTime)) ~
    // Reisezeit in weiteren Einheiten
    ("tripTimes" ->
      JArray(List(
        ("unit" -> "min") ~
        ("value" -> "%.4f".format(tripTime / 60)),
        ("unit" -> "h") ~
        ("value" -> "%.4f".format(tripTime / 3600)))))
  }

  /**
   * Erzeugt eine String-Repräsentation des Weges.
   *
   * @return String-Repräsentation des Weges.
   */
  override def toString = "#%s #%s - #%s".format(id, AntNode.nodeId(startNode), AntNode.nodeId(endNode))

  /**
   * Reisezeit.
   *
   * Zeit, die benötigt wird, um den Weg bei der maximal erlaubten Geschwindigkeit zu durchqueren.
   *
   * @return Reisezeit in Sekunden.
   */
  def tripTime = length / maxSpeed

  /**
   * Aktualisiert die maximal erlaubte Geschwindigkeit.
   *
   * @param update Aktualisierungs-Parameter.
   */
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
    val startNode = system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName,
      startNodeId))
    val endNode = system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, endNodeId))
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    if (oneWay)
      new AntOneWay(id, nodes, startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, nodes, startNode, endNode, length, maxSpeed)
  }
}
