package de.fhwedel.antscout
package osm

import xml.NodeSeq
import net.liftweb.common.Logger
import map.{Node, Way}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

/**
 * Stellt einen OpenStreetMap-Weg dar.
 *
 * @param highway highway-Tag
 * @param id id-Attribut
 * @param name name-Tag
 * @param nodes Knoten, aus denen der Weg besteht.
 * @param maxSpeed maxspeed-Tag
 */
class OsmWay(val highway: String, id: String, val name: String, override val nodes: Seq[OsmNode], val maxSpeed: Double) extends Way(id, nodes) {

  /**
   * Weg-Länge in Metern
   */
  val length = OsmWay.length(nodes)

  /**
   * Berechnet den Endknoten des Weges. Dieser ist davon abhängig, welcher Knoten als Startknoten definiert wird.
   *
   * @param startNode der als Startknoten definierte Knoten.
   * @return Endknoten
   */
  def endNode(startNode: OsmNode) = {
    if (nodes.head == startNode)
      nodes last
    else
      nodes head
  }

  /**
   * Berechnet, ob ein Weg ein Kreis-Weg ist. Das ist der Fall, wenn der Weg mit demselben Knoten anfängt und endet.
   *
   * @return True, wenn der Weg ein Kreis-Weg ist.
   */
  def isCircle = nodes.head == nodes.last

  /**
   * Berechnet, ob der übergebene Knoten der End-Knoten ist.
   *
   * @param node Knoten, der überprüft werden soll.
   * @return True, wenn der übergebene Knoten der End-Knoten ist.
   */
  def isEndNode(node: Node) = node == nodes.last

  /**
   * Berechnet, ob der übergebene Knoten der Start-Knoten ist.
   *
   * @param node Knoten, der überprüft werden soll.
   * @return True, wenn der übergebene Knoten der Start-Knoten ist.
   */
  def isStartNode(node: Node) = node == nodes.head

  override def toString = "#%s #%s - #%s".format(id, nodes.head.id, nodes.last.id)
}

object OsmWay extends Logger {
  
  val DefaultSpeed = 13.8889

  def apply(highway: String, id: Int, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmWay(highway, id.toString, name, nodes, maxSpeed)

  def apply(id: Int, nodes: List[OsmNode]) = new OsmWay("", id.toString, "", nodes, 0)

  def apply(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmWay(highway, id, name, nodes, maxSpeed)

  /**
   * Berechnet die Weg-Länge in Metern.
   *
   * @param nodes Knoten, aus denen der Weg besteht
   * @return Weg-Länge
   */
  def length(nodes: Seq[OsmNode]) = {
    nodes.zip(nodes.tail).map { case (node1, node2) =>
      node1.distanceTo(node2)
    }.sum
  }

  def parseWay(way: xml.Node, nodes: Map[String, OsmNode]): OsmWay = {
    def parseNodes(wayId: String, wayNodes: NodeSeq) = {
      wayNodes.flatMap(wayNode => {
        val wayNodeId = (wayNode \ "@ref").text
//        assert(nodes.isDefinedAt(wayNodeId), "Way id: %s, node id: %s".format(wayId, wayNodeId))
        nodes.find { case (id, _) => id == wayNodeId } map { case (_, node) => node }
      }).toList
    }
    val id = (way \ "@id").text
    val wayNodes = parseNodes(id, way \ "nd")
    val tags = (way \ "tag" map (tag => ((tag \ "@k").text, (tag \ "@v").text))).toMap
    val highway = tags getOrElse ("highway", "")
    val name = tags.getOrElse("name", "")
    def maxSpeedFromMaxSpeedTag: Option[Double] = {
      tags.getOrElse("maxspeed", "") match {
        case value if value != "" => {
          try {
            // bei einfachen Werten davon ausgehen, dass die Angabe in km/h ist und in m/s umrechnen
            Some(value.toDouble).map(_ / 3.6)
          } catch {
            case numberFormatException: NumberFormatException => {
              val maxSpeed = Settings.defaultSpeed(value)
              if (maxSpeed.isEmpty)
                warn("Way %s: unknown max speed \"%s\"" format(id, value))
              maxSpeed
            }
            case exception: Exception => {
              warn("Way %s: exception while parsing max speed \"%s\" of way %d" format(id, value), exception)
              None
            }
          }
        }
        case _ =>
          None
      }
    }
    def maxSpeedFromHighwayTag = Settings.defaultSpeed(tags.getOrElse("highway", "default"))
    val maxSpeed = maxSpeedFromMaxSpeedTag orElse maxSpeedFromHighwayTag getOrElse Settings.DefaultSpeed
    val oneWay = tags.getOrElse("oneway", "")
    oneWay match {
      // Einbahnstrasse, Richtung ist durch die Reihenfolge der Knoten vorgegeben
      case "yes" | "true" | "1" => OsmOneWay(highway, id, name, wayNodes, maxSpeed)
      // Einbahnstrasse,Richtung ist durch die umgekehrte Reihenfolge der Knoten vorgegeben
      case "-1" => OsmOneWay(highway, id, name, wayNodes.reverse, maxSpeed)
      case "no" | "false" | "0" => OsmWay(highway, id, name, wayNodes, maxSpeed)
      case value: String => {
        if (!value.isEmpty)
          warn("Way %s, unknown oneway value \"%s\"".format(id, oneWay))
        OsmWay(highway, id, name, wayNodes, maxSpeed)
      }
    }
  }
}
