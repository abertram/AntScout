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
 * Repräsentiert eine Strasse in der AntNet-Karte.
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

  /**
   * Getter für die maximale Geschwindigkeit.
   *
   * @param await Falls der Wert im Moment der Abfrage verändert wird, gibt dieses Flag an, ob gewartet werden soll,
   *              bis der Wert verändert ist.
   * @return Maximale Geschwidigkeit
   */
  def maxSpeed(implicit await: Boolean = false) = {
    if (await)
      _maxSpeed.await
    else
      _maxSpeed.get
  }

  /**
   * Setter für die maximale Geschwindigkeit.
   *
   * @param value Neue Geschwindigkeit
   */
  def maxSpeed_=(value: Double) = _maxSpeed.send(value)

  /**
   * Berechnet die Start- und End-Knoten.
   *
   * @return Start- und End-Knoten als Menge
   */
  def startAndEndNodes = Set(startNode, endNode)

  /**
   * Erzeugt eine Json-Repräsentation des Weges.
   *
   * @return Json-Repräsentation des Weges.
   */
  override def toJson = {
    super.toJson ~
    // Länge in km
    ("length" -> "%.4f".format(length / 1000)) ~
    // Länge in weiteren Einheiten
    ("lengths" ->
      JArray(List(("unit" -> "m") ~
      ("value" -> "%.4f".format(length))))) ~
    // maximal erlaubte Geschwindigkeit in km/h
    ("maxSpeed" -> "%.4f".format(maxSpeed * 3.6)) ~
    // maximal erlaubte Geschwindigkeit in weiteren Einheiten
    ("maxSpeeds" ->
      JArray(List(("unit" -> "m/s") ~
      ("value" -> "%.4f".format(maxSpeed))))) ~
    // Reisezeit in Minuten
    ("tripTime" -> "%.4f".format(tripTime / 60)) ~
    // Reisezeit in weiteren Einheiten
    ("tripTimes" ->
      JArray(List(
        ("unit" -> "s") ~
        ("value" -> "%.4f".format(tripTime)),
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
   * Zeit, die benötigt wird, um den Weg bei der aktuell maximal erlaubten Geschwindigkeit zu durchqueren.
   *
   * @return Reisezeit in Sekunden.
   */
  def tripTime = length / maxSpeed

  /**
   * Aktualisiert den Weg.
   *
   * @param update Aktualisierungs-Parameter.
   */
  def update(update: AntWay.Update) = {
    // Maximale Geschwindigkeit in m/s setzen
    maxSpeed = update.maxSpeed / 3.6
  }
}

/**
 * AntWay-Factory.
 */
object AntWay extends Logger {

  /**
   * Update-Daten für einen Weg.
   *
   * @param maxSpeed Maximale Geschwindigkeit in km/h.
   */
  case class Update(maxSpeed: Double)

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.AntWay]]-Instanz.
   *
   * @param id Eindeutige Id
   * @param startNode Start-Knoten
   * @param endNode End-Knoten
   * @param length Länge
   * @param maxSpeed Maximale Geschwidigkeit
   * @return Neue [[de.fhwedel.antscout.antnet.AntWay]]-Instanz
   */
  def apply(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) = {
    new AntWay(id, Seq(), startNode, endNode, length, maxSpeed)
  }

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.AntWay]]-Instanz.
   *
   * @param id Eindeutige Id
   * @param nodes Osm-Knoten-Sequenz
   * @param maxSpeed Maximale Geschwidigkeit
   * @param oneWay Flag, ob Einbahn-Strasse
   * @return Neue [[de.fhwedel.antscout.antnet.AntWay]]-Instanz
   */
  def apply(id: String, nodes: Seq[OsmNode], maxSpeed: Double, oneWay: Boolean = false) = {
    // Start- und End-Knoten-Aktoren berechnen
    val startNode = AntNode(nodes.head.id)
    val endNode = AntNode(nodes.last.id)
    // Länge berechnen
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    // Ant-Weg erzeugen
    if (oneWay)
      new AntOneWay(id, nodes, startNode, endNode, length, maxSpeed)
    else
      new AntWay(id, nodes, startNode, endNode, length, maxSpeed)
  }
}
